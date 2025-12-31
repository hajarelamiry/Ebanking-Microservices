package com.example.account_service.service;

import com.example.account_service.dto.TransactionDto;

import com.example.account_service.dto.*;
import java.math.BigDecimal;

public interface AccountService {

    /**
     * Crée un nouveau compte pour un utilisateur dans une devise spécifique.
     */
    AccountDto createAccount(CreateAccountRequestDto request, Long userId);

    /**
     * Récupère le solde actuel d'un compte appartenant à l'utilisateur.
     */
    SoldeResponseDto consulterSolde(String accountRef, Long userId);

    /**
     * Ajoute des fonds sur un compte.
     */
    AccountDto creditAccount(String accountRef, BigDecimal amount, Long userId);

    /**
     * Retire des fonds d'un compte.
     */
    AccountDto debitAccount(String accountRef, BigDecimal amount, Long userId);

    /**
     * Effectue un virement entre deux comptes (avec conversion de devise si nécessaire).
     */
    TransactionDto processPayment(PaymentRequestDto request, Long userId);
}