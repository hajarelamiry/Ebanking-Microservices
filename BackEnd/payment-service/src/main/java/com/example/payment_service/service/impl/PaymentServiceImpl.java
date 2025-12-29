package com.example.payment_service.service.impl;

import com.example.payment_service.dto.PaymentRequestDTO;
import com.example.payment_service.dto.PaymentResponseDTO;
import com.example.payment_service.enums.TransactionStatus;
import com.example.payment_service.model.Payment;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.FraudDetectionService;
import com.example.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implémentation du service de gestion des paiements
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final FraudDetectionService fraudDetectionService;

    @Override
    @Transactional
    public PaymentResponseDTO initiatePayment(PaymentRequestDTO requestDTO) {
        log.info("Initiation d'un virement: compte source={}, montant={}, type={}", 
                requestDTO.getSourceAccountId(), requestDTO.getAmount(), requestDTO.getType());

        // 1. Vérification des règles anti-fraude
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

        // 4. Simulation d'appel au legacy-adapter-service (seulement si PENDING)
        if (initialStatus == TransactionStatus.PENDING) {
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
    }
}

