package com.example.payment_service.service;

import com.example.payment_service.dto.*;
import com.example.payment_service.enums.TransactionStatus;
import com.example.payment_service.enums.TransactionType;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.repositories.TransactionRepository;
import com.example.payment_service.service.AccountService;
import com.example.payment_service.service.LegacyBankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Orchestrateur Saga pour gérer les transactions distribuées
 * Pattern Saga orchestré pour garantir la cohérence transactionnelle
 * 
 * IMPORTANT: Ne dépend PAS de PaymentService pour éviter les dépendances circulaires
 * Utilise directement les repositories et clients Feign
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final LegacyBankingService legacyBankingService;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;
    private final KafkaEventProducer kafkaEventProducer;

    /**
     * Envoie un virement au legacy adapter service
     */
    public void sendToLegacy(Transaction transaction, PaymentRequest request) {
        log.info("Envoi de la transaction {} au legacy adapter", transaction.getId());

        try {
            LegacyPaymentRequest legacyRequest = LegacyPaymentRequest.builder()
                    .transactionId(transaction.getId())
                    .sourceAccountId(request.getSourceAccountId())
                    .destinationIban(request.getDestinationIban())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .build();

            LegacyPaymentResponse legacyResponse = legacyBankingService.sendPayment(legacyRequest);

            if (legacyResponse != null && legacyResponse.getSuccess()) {
                // Succès - publier l'événement de complétion
                SagaEvent successEvent = SagaEvent.builder()
                        .sagaId(UUID.randomUUID())
                        .transactionId(transaction.getId())
                        .eventType("LEGACY_PAYMENT_SENT")
                        .transactionType(transaction.getType())
                        .status(TransactionStatus.COMPLETED.name())
                        .timestamp(LocalDateTime.now())
                        .build();
                kafkaEventProducer.publishSagaEvent(successEvent);

                // Mettre à jour le statut de la transaction directement
                updateTransactionStatus(transaction.getId(), TransactionStatus.COMPLETED);
                log.info("Transaction {} envoyée avec succès au legacy. Référence: {}", 
                        transaction.getId(), legacyResponse.getLegacyReference());
            } else {
                // Échec - déclencher la compensation
                handleLegacyFailure(transaction, request, 
                        legacyResponse != null ? legacyResponse.getMessage() : "Erreur inconnue du legacy");
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi au legacy adapter: {}", e.getMessage(), e);
            handleLegacyFailure(transaction, request, e.getMessage());
        }
    }

    /**
     * Gère l'échec de l'envoi au legacy et déclenche la compensation
     */
    private void handleLegacyFailure(Transaction transaction, PaymentRequest request, String errorMessage) {
        log.error("Échec de l'envoi au legacy pour la transaction {}. Erreur: {}", 
                transaction.getId(), errorMessage);

        // Publier l'événement d'échec
        SagaEvent failureEvent = SagaEvent.builder()
                .sagaId(UUID.randomUUID())
                .transactionId(transaction.getId())
                .eventType("LEGACY_PAYMENT_FAILED")
                .transactionType(transaction.getType())
                .errorMessage(errorMessage)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaEventProducer.publishSagaEvent(failureEvent);

        // Si c'est un virement instantané, compenser (recréditer le compte)
        if (transaction.getType() == TransactionType.INSTANT) {
            compensateInstantPayment(transaction, request);
        } else {
            // Pour les virements standard, juste mettre à jour le statut
            updateTransactionStatus(transaction.getId(), TransactionStatus.REJECTED);
        }
    }

    /**
     * Compense un virement instantané en recréditant le compte
     */
    private void compensateInstantPayment(Transaction transaction, PaymentRequest request) {
        log.info("Démarrage de la compensation pour la transaction {}", transaction.getId());

        // Publier l'événement de compensation
        SagaEvent compensationEvent = SagaEvent.builder()
                .sagaId(UUID.randomUUID())
                .transactionId(transaction.getId())
                .eventType("COMPENSATION_STARTED")
                .transactionType(TransactionType.INSTANT)
                .timestamp(LocalDateTime.now())
                .build();
        kafkaEventProducer.publishSagaEvent(compensationEvent);

        // Compenser la transaction directement
        compensateTransaction(
                transaction.getId(),
                request.getAmount(),
                request.getCurrency()
        );

        log.info("Compensation terminée pour la transaction {}", transaction.getId());
    }

    /**
     * Met à jour le statut d'une transaction (méthode interne du Saga)
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
     * Compense une transaction (rollback) - méthode interne du Saga
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

    /**
     * Traite un événement Saga reçu (pour les événements asynchrones)
     */
    public void handleSagaEvent(SagaEvent event) {
        log.info("Traitement de l'événement Saga: {} pour la transaction {}", 
                event.getEventType(), event.getTransactionId());

        switch (event.getEventType()) {
            case "LEGACY_PAYMENT_SENT":
                updateTransactionStatus(
                        event.getTransactionId(), 
                        TransactionStatus.COMPLETED
                );
                break;
            case "LEGACY_PAYMENT_FAILED":
                if (event.getTransactionType() == TransactionType.INSTANT) {
                    // La compensation sera gérée par handleLegacyFailure
                } else {
                    updateTransactionStatus(
                            event.getTransactionId(), 
                            TransactionStatus.REJECTED
                    );
                }
                break;
            default:
                log.debug("Événement Saga non géré: {}", event.getEventType());
        }
    }
}

