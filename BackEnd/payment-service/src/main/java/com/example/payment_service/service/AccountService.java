package com.example.payment_service.service;

import com.example.payment_service.dto.AccountBalanceResponse;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Interface de service pour les opérations sur les comptes
 * Permet de remplacer facilement l'implémentation (Mock ou Réelle)
 */
public interface AccountService {

    /**
     * Vérifie le solde d'un compte
     */
    AccountBalanceResponse getAccountBalance(UUID accountId);

    /**
     * Débite un compte
     */
    AccountBalanceResponse debitAccount(UUID accountId, BigDecimal amount, String currency, UUID transactionId);

    /**
     * Crédite un compte
     */
    AccountBalanceResponse creditAccount(UUID accountId, BigDecimal amount, String currency, UUID transactionId);

    /**
     * Vérifie si le solde est suffisant
     */
    Boolean checkSufficientBalance(UUID accountId, BigDecimal amount);
}

