package com.example.payment_service.service.impl;

import com.example.payment_service.dto.PaymentRequestDTO;
import com.example.payment_service.enums.TransactionStatus;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.FraudDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Implémentation du service de détection de fraude
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private final PaymentRepository paymentRepository;
    
    // Seuils anti-fraude
    private static final Double FRAUD_THRESHOLD = 10000.0; // Seuil anti-fraude : 10 000€
    private static final int MAX_TRANSACTIONS_IN_10_MIN = 3; // Maximum 3 virements en 10 minutes
    private static final int VELOCITY_WINDOW_MINUTES = 10; // Fenêtre de temps pour la vélocité
    private static final Double NEW_BENEFICIARY_THRESHOLD = 2000.0; // Seuil pour nouveau bénéficiaire : 2 000€
    private static final Double DAILY_CUMULATIVE_THRESHOLD = 15000.0; // Seuil cumul journalier : 15 000€

    @Override
    public FraudCheckResult checkFraudRules(PaymentRequestDTO requestDTO) {
        // Règle 1 : Montant > 10 000€ → REJECTED
        if (requestDTO.getAmount() > FRAUD_THRESHOLD) {
            log.warn("⚠️ Règle anti-fraude 1 déclenchée: montant {}€ dépasse le seuil de {}€", 
                    requestDTO.getAmount(), FRAUD_THRESHOLD);
            return new FraudCheckResult(
                TransactionStatus.REJECTED,
                String.format("Transaction rejetée: montant supérieur au seuil autorisé (%.2f€ > %.2f€)", 
                        requestDTO.getAmount(), FRAUD_THRESHOLD)
            );
        }

        // Règle 2 : Plus de 3 virements en 10 minutes → REJECTED
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);
        long transactionCount = paymentRepository.countBySourceAccountIdAndCreatedAtAfter(
                requestDTO.getSourceAccountId(), tenMinutesAgo);
        
        if (transactionCount >= MAX_TRANSACTIONS_IN_10_MIN) {
            log.warn("⚠️ Règle anti-fraude 2 déclenchée: {} virements détectés dans les {} dernières minutes (seuil: {})", 
                    transactionCount, VELOCITY_WINDOW_MINUTES, MAX_TRANSACTIONS_IN_10_MIN);
            return new FraudCheckResult(
                TransactionStatus.REJECTED,
                String.format("Transaction rejetée: trop de virements récents (%d virements en %d minutes, maximum autorisé: %d)", 
                        transactionCount, VELOCITY_WINDOW_MINUTES, MAX_TRANSACTIONS_IN_10_MIN)
            );
        }

        // Règle 4 : Cumul journalier > 15 000€ → REJECTED
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        double dailyTotal = paymentRepository.sumAmountBySourceAccountIdAndCreatedAtAfter(
                requestDTO.getSourceAccountId(), startOfDay);
        double newTotal = dailyTotal + requestDTO.getAmount();
        
        if (newTotal > DAILY_CUMULATIVE_THRESHOLD) {
            log.warn("⚠️ Règle anti-fraude 4 déclenchée: cumul journalier {}€ + montant {}€ = {}€ dépasse le seuil de {}€", 
                    dailyTotal, requestDTO.getAmount(), newTotal, DAILY_CUMULATIVE_THRESHOLD);
            return new FraudCheckResult(
                TransactionStatus.REJECTED,
                String.format("Transaction rejetée: cumul journalier dépassé (%.2f€ + %.2f€ = %.2f€ > %.2f€)", 
                        dailyTotal, requestDTO.getAmount(), newTotal, DAILY_CUMULATIVE_THRESHOLD)
            );
        }

        // Règle 3 : Nouveau bénéficiaire (IBAN jamais utilisé) ET montant > 2 000€ → PENDING_MANUAL_REVIEW
        boolean isNewBeneficiary = !paymentRepository.existsBySourceAccountIdAndDestinationIban(
                requestDTO.getSourceAccountId(), requestDTO.getDestinationIban());
        
        if (isNewBeneficiary && requestDTO.getAmount() > NEW_BENEFICIARY_THRESHOLD) {
            log.warn("⚠️ Règle anti-fraude 3 déclenchée: nouveau bénéficiaire (IBAN: {}) avec montant {}€ > {}€", 
                    requestDTO.getDestinationIban(), requestDTO.getAmount(), NEW_BENEFICIARY_THRESHOLD);
            return new FraudCheckResult(
                TransactionStatus.PENDING_MANUAL_REVIEW,
                String.format("Transaction en attente de validation manuelle: nouveau bénéficiaire avec montant supérieur à %.2f€ (%.2f€)", 
                        NEW_BENEFICIARY_THRESHOLD, requestDTO.getAmount())
            );
        }

        // Aucune règle déclenchée → PENDING
        return new FraudCheckResult(
            TransactionStatus.PENDING,
            "Transaction créée avec succès"
        );
    }
}

