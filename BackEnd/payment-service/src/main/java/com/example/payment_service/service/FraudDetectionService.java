package com.example.payment_service.service;

import com.example.payment_service.dto.PaymentRequestDTO;
import com.example.payment_service.enums.TransactionStatus;

/**
 * Service dédié à la détection de fraude
 */
public interface FraudDetectionService {

    /**
     * Vérifie toutes les règles anti-fraude pour une transaction
     * 
     * @param requestDTO La requête de virement à vérifier
     * @return Le résultat de la vérification avec le statut et le message
     */
    FraudCheckResult checkFraudRules(PaymentRequestDTO requestDTO);

    /**
     * Résultat d'une vérification anti-fraude
     */
    class FraudCheckResult {
        private final TransactionStatus status;
        private final String message;

        public FraudCheckResult(TransactionStatus status, String message) {
            this.status = status;
            this.message = message;
        }

        public TransactionStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }
    }
}

