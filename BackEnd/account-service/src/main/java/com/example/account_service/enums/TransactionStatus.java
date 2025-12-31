package com.example.account_service.enums;

public enum TransactionStatus {

    PENDING,     // Transaction créée, en attente
    VALIDATED,   // Succès (argent transféré)
    REJECTED,    // Refusée (solde insuffisant, règle violée)
    CANCELLED,   // Annulée par l'utilisateur ou le système
    FAILED       // Erreur technique
}