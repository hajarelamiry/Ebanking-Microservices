package com.example.payment_service.service;

import com.example.payment_service.dto.LegacyPaymentRequest;
import com.example.payment_service.dto.LegacyPaymentResponse;

/**
 * Interface de service pour les opérations bancaires legacy
 * Permet de remplacer facilement l'implémentation (Mock ou Réelle)
 */
public interface LegacyBankingService {

    /**
     * Envoie un virement au système legacy (SOAP)
     */
    LegacyPaymentResponse sendPayment(LegacyPaymentRequest request);
}

