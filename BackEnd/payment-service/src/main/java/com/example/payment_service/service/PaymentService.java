package com.example.payment_service.service;

import com.example.payment_service.service.AccountService;
import com.example.payment_service.service.LegacyBankingService;
import com.example.payment_service.dto.*;
import com.example.payment_service.enums.TransactionStatus;
import com.example.payment_service.enums.TransactionType;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service principal pour la gestion des virements
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final LegacyBankingService legacyBankingService;
    private final SagaOrchestrator sagaOrchestrator;
    private final KafkaEventProducer kafkaEventProducer;
    private final FraudDetectionService fraudDetectionService;

    /**
     * Traite un virement (Standard ou Instantané)
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Traitement d'un virement de type {} pour le compte {}", request.getType(), request.getSourceAccountId());

        // Créer la transaction en base avec statut PENDING
        Transaction transaction = Transaction.builder()
                .sourceAccountId(request.getSourceAccountId())
                .destinationIban(request.getDestinationIban())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .type(request.getType())
                .status(TransactionStatus.PENDING)
                .build();

        transaction = transactionRepository.save(transaction);
        log.info("Transaction créée avec l'ID: {}", transaction.getId());

        // Étape 1 : Détection de fraude AVANT le traitement
        var fraudResult = fraudDetectionService.detectFraud(transaction, request);
        
        if (fraudResult.isFraudSuspected()) {
            log.warn("Transaction {} suspectée de fraude. Score: {}, Règle violée: {}", 
                    transaction.getId(), fraudResult.getRiskScore(), fraudResult.getViolatedRule());
            
            transaction.setStatus(TransactionStatus.FRAUD_SUSPECTED);
            transactionRepository.save(transaction);
            
            // Publier l'événement de fraude suspectée
            SagaEvent fraudEvent = SagaEvent.builder()
                    .sagaId(UUID.randomUUID())
                    .transactionId(transaction.getId())
                    .eventType("FRAUD_SUSPECTED")
                    .transactionType(request.getType())
                    .errorMessage("Transaction suspectée de fraude: " + fraudResult.getViolatedRule())
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaEventProducer.publishSagaEvent(fraudEvent);
            
            return PaymentResponse.builder()
                    .transactionId(transaction.getId())
                    .status(TransactionStatus.FRAUD_SUSPECTED)
                    .message("Transaction suspectée de fraude. Validation manuelle requise. Règle violée: " + 
                            fraudResult.getViolatedRule())
                    .createdAt(transaction.getCreatedAt())
                    .build();
        }

        // Publier l'événement de création de transaction
        SagaEvent event = SagaEvent.builder()
                .sagaId(UUID.randomUUID())
                .transactionId(transaction.getId())
                .eventType("TRANSACTION_CREATED")
                .transactionType(request.getType())
                .sourceAccountId(request.getSourceAccountId())
                .destinationIban(request.getDestinationIban())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(TransactionStatus.PENDING.name())
                .timestamp(LocalDateTime.now())
                .build();
        kafkaEventProducer.publishSagaEvent(event);

        try {
            if (request.getType() == TransactionType.INSTANT) {
                return processInstantPayment(transaction, request);
            } else {
                return processStandardPayment(transaction, request);
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement du virement: {}", e.getMessage(), e);
            transaction.setStatus(TransactionStatus.REJECTED);
            transactionRepository.save(transaction);
            
            // Publier l'événement d'échec
            SagaEvent failureEvent = SagaEvent.builder()
                    .sagaId(event.getSagaId())
                    .transactionId(transaction.getId())
                    .eventType("TRANSACTION_FAILED")
                    .transactionType(request.getType())
                    .errorMessage(e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
            kafkaEventProducer.publishSagaEvent(failureEvent);
            
            return PaymentResponse.builder()
                    .transactionId(transaction.getId())
                    .status(TransactionStatus.REJECTED)
                    .message("Échec du virement: " + e.getMessage())
                    .createdAt(transaction.getCreatedAt())
                    .build();
        }
    }

    /**
     * Traite un virement instantané
     * - Vérifie le solde en temps réel
     * - Débite immédiatement le compte
     * - Envoie au legacy pour exécution
     */
    private PaymentResponse processInstantPayment(Transaction transaction, PaymentRequest request) {
        log.info("Traitement d'un virement INSTANT pour la transaction {}", transaction.getId());

        // 1. Vérifier le solde en temps réel
        AccountBalanceResponse balanceResponse = accountService.getAccountBalance(request.getSourceAccountId());
        if (balanceResponse == null || !balanceResponse.getSufficientFunds()) {
            throw new RuntimeException("Solde insuffisant pour effectuer le virement");
        }

        // 2. Débiter le compte immédiatement
        AccountBalanceResponse debitResponse = accountService.debitAccount(
                request.getSourceAccountId(),
                request.getAmount(),
                request.getCurrency(),
                transaction.getId()
        );

        if (debitResponse == null) {
            throw new RuntimeException("Échec du débit du compte");
        }

        log.info("Compte débité avec succès. Nouveau solde: {}", debitResponse.getBalance());

        // Publier l'événement de débit
        SagaEvent debitEvent = SagaEvent.builder()
                .sagaId(UUID.randomUUID())
                .transactionId(transaction.getId())
                .eventType("ACCOUNT_DEBITED")
                .transactionType(TransactionType.INSTANT)
                .sourceAccountId(request.getSourceAccountId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .timestamp(LocalDateTime.now())
                .build();
        kafkaEventProducer.publishSagaEvent(debitEvent);

        // 3. Envoyer au legacy adapter (asynchrone via Saga)
        sagaOrchestrator.sendToLegacy(transaction, request);

        // Mettre à jour le statut
        transaction.setStatus(TransactionStatus.VALIDATED);
        transactionRepository.save(transaction);

        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .status(TransactionStatus.VALIDATED)
                .message("Virement instantané validé et en cours de traitement")
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * Traite un virement standard
     * - Enregistre en base
     * - Envoie au legacy adapter pour traitement asynchrone (batch)
     */
    private PaymentResponse processStandardPayment(Transaction transaction, PaymentRequest request) {
        log.info("Traitement d'un virement STANDARD pour la transaction {}", transaction.getId());

        // Envoyer au legacy adapter via Saga (traitement asynchrone)
        sagaOrchestrator.sendToLegacy(transaction, request);

        // Mettre à jour le statut
        transaction.setStatus(TransactionStatus.VALIDATED);
        transactionRepository.save(transaction);

        return PaymentResponse.builder()
                .transactionId(transaction.getId())
                .status(TransactionStatus.VALIDATED)
                .message("Virement standard enregistré et en cours de traitement")
                .createdAt(transaction.getCreatedAt())
                .build();
    }

    /**
     * Met à jour le statut d'une transaction (appelé par le Saga)
     */
    @Transactional
    public void updateTransactionStatus(UUID transactionId, TransactionStatus status) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée: " + transactionId));
        
        transaction.setStatus(status);
        transactionRepository.save(transaction);
        log.info("Statut de la transaction {} mis à jour: {}", transactionId, status);
    }

    /**
     * Compense une transaction (rollback)
     */
    @Transactional
    public void compensateTransaction(UUID transactionId, BigDecimal amount, String currency) {
        log.info("Compensation de la transaction {}", transactionId);
        
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction non trouvée: " + transactionId));

        // Créditer le compte source
        accountService.creditAccount(
                transaction.getSourceAccountId(),
                amount,
                currency,
                transactionId
        );

        // Mettre à jour le statut
        transaction.setStatus(TransactionStatus.REJECTED);
        transactionRepository.save(transaction);

        log.info("Transaction {} compensée avec succès", transactionId);
    }
}

