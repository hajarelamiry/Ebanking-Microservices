package org.example.enums;

/**
 * Enumération représentant les différents fournisseurs ou catégories de services
 * pour lesquels des paiements récurrents peuvent être effectués.
 */
public enum Fournisseur {
    // Opérateurs télécom
    MAROC_TELECOM,       // Paiement à Maroc Télécom
    INWI,                // Paiement à Inwi
    ORANGE,              // Paiement à Orange

    // Fournisseurs d'eau et d'électricité
    AMENDIS,             // Paiement pour Amendis (eau et électricité)
    LYDEC,               // Paiement pour Lydec (eau et électricité)
    REDAL,               // Paiement pour Redal (eau et électricité)

    // Factures spécifiques
    FACTURE_INTERNET,    // Paiement de facture Internet
    FACTURE_ELECTRICITE, // Paiement de facture d'électricité
    FACTURE_EAU,         // Paiement de facture d'eau

    // Assurances
    ASSURANCE_VOITURE,   // Paiement pour assurance automobile
    ASSURANCE_HABITATION,// Paiement pour assurance habitation

    // Loisirs et abonnements
    COTISATION_GYM,      // Cotisation pour salle de sport
    ABONNEMENT_NETFLIX,  // Paiement pour abonnement Netflix
    ABONNEMENT_SPOTIFY,  // Paiement pour abonnement Spotify
    ABONNEMENT_AMAZON,   // Paiement pour abonnement Amazon Prime

    // Services de transport
    TRANSPORT,           // Paiement pour transport (carte de bus, train, etc.)

    // Besoins courants
    LOYER,               // Paiement du loyer
    SCOLARITE,           // Paiement des frais de scolarité

    // Autres
    AUTRE                // Autres paiements récurrents non spécifiés
}
