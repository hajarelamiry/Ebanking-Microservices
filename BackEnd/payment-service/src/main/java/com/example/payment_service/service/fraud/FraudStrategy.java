package com.example.payment_service.service.fraud;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.model.Transaction;

/**
 * Interface pour les stratégies de détection de fraude
 */
public interface FraudStrategy {

    /**
     * Évalue le risque de fraude pour une transaction
     * @param transaction La transaction à évaluer
     * @param request La requête de paiement
     * @return Le score de risque (0.0 = aucun risque, 100.0 = risque maximum)
     */
    double evaluateRisk(Transaction transaction, PaymentRequest request);

    /**
     * Retourne le nom de la règle violée si applicable
     * @return Le nom de la règle ou null si aucune violation
     */
    String getViolatedRule();

    /**
     * Retourne le nom de la stratégie
     */
    String getStrategyName();
}

