package com.example.payment_service.service.impl;

import com.example.payment_service.dto.AuditEventDTO;
import com.example.payment_service.dto.PaymentRequestDTO;
import com.example.payment_service.dto.PaymentResponseDTO;
import com.example.payment_service.enums.TransactionStatus;
import com.example.payment_service.model.Payment;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.AccountService;
import com.example.payment_service.service.AuditService;
import com.example.payment_service.service.FraudDetectionService;
import com.example.payment_service.service.PaymentService;
import com.example.payment_service.util.CorrelationIdContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Implémentation du service de gestion des paiements
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final FraudDetectionService fraudDetectionService;
    private final AuditService auditService; // Communication synchrone via Eureka/Feign
    private final AccountService accountService; // Communication avec Account Service via Feign

    @Override
    @Transactional
    public PaymentResponseDTO initiatePayment(PaymentRequestDTO requestDTO) {
        log.info("Initiation d'un virement: compte source={}, montant={}, type={}", 
                requestDTO.getSourceAccountId(), requestDTO.getAmount(), requestDTO.getType());

        // 1. Vérification du solde du compte via Account Service
        BigDecimal requiredAmount = BigDecimal.valueOf(requestDTO.getAmount());
        boolean hasSufficientBalance = accountService.hasSufficientBalance(
                requestDTO.getSourceAccountId(), requiredAmount);
        
        if (!hasSufficientBalance) {
            BigDecimal balance = accountService.getBalance(requestDTO.getSourceAccountId());
            String errorMessage = String.format(
                    "Insufficient balance. Account: %s, Balance: %s, Required: %s", 
                    requestDTO.getSourceAccountId(), balance, requiredAmount);
            log.warn("❌ Payment rejected: {}", errorMessage);
            
            // Créer un paiement avec statut REJECTED
            Payment payment = Payment.builder()
                    .sourceAccountId(requestDTO.getSourceAccountId())
                    .destinationIban(requestDTO.getDestinationIban())
                    .amount(requestDTO.getAmount())
                    .type(requestDTO.getType())
                    .status(TransactionStatus.REJECTED)
                    .build();
            payment = paymentRepository.save(payment);
            
            // Envoyer l'événement d'audit
            sendAuditEventViaFeign(payment, TransactionStatus.REJECTED, errorMessage);
            
            return PaymentResponseDTO.builder()
                    .id(payment.getId())
                    .sourceAccountId(payment.getSourceAccountId())
                    .destinationIban(payment.getDestinationIban())
                    .amount(payment.getAmount())
                    .status(payment.getStatus())
                    .message(errorMessage)
                    .createdAt(payment.getCreatedAt())
                    .build();
        }

        // 2. Vérification des règles anti-fraude
        FraudDetectionService.FraudCheckResult fraudCheck = fraudDetectionService.checkFraudRules(requestDTO);
        TransactionStatus initialStatus = fraudCheck.getStatus();
        String message = fraudCheck.getMessage();

        // 2. Création de l'entité Payment
        Payment payment = Payment.builder()
                .sourceAccountId(requestDTO.getSourceAccountId())
                .destinationIban(requestDTO.getDestinationIban())
                .amount(requestDTO.getAmount())
                .type(requestDTO.getType())
                .status(initialStatus)
                .build();

        // 3. Enregistrement en base de données
        payment = paymentRepository.save(payment);
        log.info("Transaction enregistrée avec l'ID: {} et le statut: {}", payment.getId(), payment.getStatus());

        // 4. Publication de l'événement d'audit via Feign Client/Eureka (synchrone)
        sendAuditEventViaFeign(payment, initialStatus, message);

        // 5. Débiter le compte si le paiement est validé
        if (initialStatus == TransactionStatus.PENDING) {
            // Débiter le compte avant de simuler l'appel legacy
            try {
                accountService.debitAccount(requestDTO.getSourceAccountId(), requiredAmount);
                log.info("Account {} debited with {} EUR for payment {}", 
                        requestDTO.getSourceAccountId(), requiredAmount, payment.getId());
            } catch (Exception e) {
                log.error("Failed to debit account {}: {}", requestDTO.getSourceAccountId(), e.getMessage());
                // Mettre à jour le statut du paiement en REJECTED
                payment.setStatus(TransactionStatus.REJECTED);
                paymentRepository.save(payment);
                sendAuditEventViaFeign(payment, TransactionStatus.REJECTED, 
                        "Payment rejected: Failed to debit account - " + e.getMessage());
                throw new RuntimeException("Failed to process payment: " + e.getMessage(), e);
            }
            
            // Simulation d'appel au legacy-adapter-service
            simulateLegacyAdapterCall(payment);
        }

        // 5. Construction de la réponse
        return PaymentResponseDTO.builder()
                .id(payment.getId())
                .sourceAccountId(payment.getSourceAccountId())
                .destinationIban(payment.getDestinationIban())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .message(message)
                .createdAt(payment.getCreatedAt())
                .build();
    }


    /**
     * Simule un appel au legacy-adapter-service
     * Dans une implémentation réelle, ceci serait un appel HTTP/Feign
     */
    private void simulateLegacyAdapterCall(Payment payment) {
        log.info("🟡 [SIMULATION] Appel SOAP simulé au legacy-adapter-service");
        log.info("   - Transaction ID: {}", payment.getId());
        log.info("   - Compte source: {}", payment.getSourceAccountId());
        log.info("   - IBAN destination: {}", payment.getDestinationIban());
        log.info("   - Montant: {}€", payment.getAmount());
        log.info("   - Type: {}", payment.getType());
        log.info("🟡 [SIMULATION] ✅ Virement simulé traité avec succès");
        
        // Simulation : mise à jour du statut après "traitement" par le legacy
        payment.setStatus(TransactionStatus.VALIDATED);
        paymentRepository.save(payment);
        log.info("Statut de la transaction {} mis à jour: {}", payment.getId(), TransactionStatus.VALIDATED);
        
        // Publication de l'événement d'audit pour la validation via Feign
        sendAuditEventForValidation(payment);
    }


    /**
     * Envoie un événement d'audit via Feign Client (Eureka)
     * Appel synchrone direct à l'Audit Service
     */
    private void sendAuditEventViaFeign(Payment payment, TransactionStatus status, String message) {
        String eventType = switch (status) {
            case REJECTED -> "PAYMENT_REJECTED";
            case PENDING_MANUAL_REVIEW -> "PAYMENT_PENDING_MANUAL_REVIEW";
            default -> "PAYMENT_CREATED";
        };
        
        String auditStatus = (status == TransactionStatus.REJECTED) ? "FAILURE" : "SUCCESS";
        
        AuditEventDTO auditEvent = AuditEventDTO.builder()
                .correlationId(CorrelationIdContext.getCorrelationId())
                .userId(payment.getSourceAccountId())
                .actionType(eventType)
                .serviceName("payment-service")
                .description("Payment transaction " + status.name().toLowerCase())
                .status(auditStatus)
                .errorMessage(status == TransactionStatus.REJECTED ? message : null)
                .timestamp(LocalDateTime.now())
                .paymentId(payment.getId())
                .sourceAccountId(payment.getSourceAccountId())
                .destinationIban(payment.getDestinationIban())
                .amount(payment.getAmount())
                .transactionType(payment.getType().name())
                .transactionStatus(payment.getStatus().name())
                .build();
        
        // Envoie via Feign Client (Eureka découvre automatiquement audit-service)
        auditService.sendAuditEvent(auditEvent);
    }

    /**
     * Envoie un événement d'audit pour la validation d'un paiement
     */
    private void sendAuditEventForValidation(Payment payment) {
        AuditEventDTO auditEvent = AuditEventDTO.builder()
                .correlationId(CorrelationIdContext.getCorrelationId())
                .userId(payment.getSourceAccountId())
                .actionType("PAYMENT_VALIDATED")
                .serviceName("payment-service")
                .description("Payment transaction validated by legacy adapter")
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .paymentId(payment.getId())
                .sourceAccountId(payment.getSourceAccountId())
                .destinationIban(payment.getDestinationIban())
                .amount(payment.getAmount())
                .transactionType(payment.getType().name())
                .transactionStatus(payment.getStatus().name())
                .build();
        
        // Envoie via Feign Client (Eureka découvre automatiquement audit-service)
        auditService.sendAuditEvent(auditEvent);
    }
}

