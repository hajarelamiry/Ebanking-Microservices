package com.example.payment_service.service;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.enums.TransactionStatus;
import com.example.payment_service.model.FraudCheck;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.repositories.FraudCheckRepository;
import com.example.payment_service.service.fraud.FraudStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service de détection de fraude
 * Utilise plusieurs stratégies pour évaluer le risque d'une transaction
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private final List<FraudStrategy> fraudStrategies;
    private final FraudCheckRepository fraudCheckRepository;
    private static final double FRAUD_THRESHOLD = 70.0; // Seuil pour marquer comme suspect

    /**
     * Évalue le risque de fraude pour une transaction
     * @param transaction La transaction à évaluer
     * @param request La requête de paiement
     * @return Le résultat de la détection de fraude
     */
    @Transactional
    public FraudDetectionResult detectFraud(Transaction transaction, PaymentRequest request) {
        log.info("Démarrage de la détection de fraude pour la transaction {}", transaction.getId());

        double totalRiskScore = 0.0;
        String violatedRule = null;
        String decision = "APPROVED";

        // Évaluer avec toutes les stratégies
        for (FraudStrategy strategy : fraudStrategies) {
            double riskScore = strategy.evaluateRisk(transaction, request);
            totalRiskScore = Math.max(totalRiskScore, riskScore); // Prendre le score maximum

            if (strategy.getViolatedRule() != null) {
                violatedRule = strategy.getViolatedRule();
            }

            log.debug("Stratégie {}: score de risque = {}", strategy.getStrategyName(), riskScore);
        }

        // Déterminer la décision
        if (totalRiskScore >= FRAUD_THRESHOLD) {
            decision = "FRAUD_SUSPECTED";
            log.warn("Transaction {} marquée comme suspecte. Score de risque: {}", 
                    transaction.getId(), totalRiskScore);
        } else if (totalRiskScore > 0) {
            decision = "REVIEW_REQUIRED";
            log.info("Transaction {} nécessite une revue. Score de risque: {}", 
                    transaction.getId(), totalRiskScore);
        }

        // Enregistrer le résultat de la vérification
        FraudCheck fraudCheck = FraudCheck.builder()
                .transaction(transaction)
                .riskScore(totalRiskScore)
                .violatedRule(violatedRule)
                .decision(decision)
                .build();

        fraudCheckRepository.save(fraudCheck);

        // Retourner le résultat
        return FraudDetectionResult.builder()
                .riskScore(totalRiskScore)
                .violatedRule(violatedRule)
                .decision(decision)
                .isFraudSuspected(totalRiskScore >= FRAUD_THRESHOLD)
                .build();
    }

    /**
     * Résultat de la détection de fraude
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FraudDetectionResult {
        private double riskScore;
        private String violatedRule;
        private String decision;
        private boolean isFraudSuspected;
    }
}

