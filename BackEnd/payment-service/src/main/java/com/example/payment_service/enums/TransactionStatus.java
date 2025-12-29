package com.example.payment_service.enums;

/**
 * Statut d'une transaction
 */
public enum TransactionStatus {
    PENDING,
    VALIDATED,
    REJECTED,
    COMPLETED,
    FRAUD_SUSPECTED
}

