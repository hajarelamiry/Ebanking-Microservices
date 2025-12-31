package com.example.account_service.enums;

public enum TransactionType {
    // Argent qui entre sur le compte
    DEPOT,          // Dépôt d'argent (Cash ou virement externe)
    VIREMENT_RECU,  // Argent reçu d'un autre utilisateur

    // Argent qui sort du compte
    RETRAIT,        // Retrait d'argent
    VIREMENT_ENVOYE,// Argent envoyé à un autre utilisateur

    // Pour votre méthode processPayment (Virement entre comptes)
    VIREMENT,

    // Pour les futures fonctionnalités
    PAIEMENT_FACTURE,
    FRAIS_BANCAIRES
}