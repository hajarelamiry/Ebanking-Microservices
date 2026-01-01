package com.example.account_service.service;

import com.example.account_service.dto.TransactionDto;

import com.example.account_service.dto.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface AccountService {

    /**
     * Crée un nouveau compte pour un utilisateur dans une devise spécifique.
     */
    AccountDto createAccount(CreateAccountRequestDto request, String  userId);

    /**
     * Récupère le solde actuel d'un compte appartenant à l'utilisateur.
     */
    SoldeResponseDto consulterSolde(String accountRef, String userId);

    /**
     * Ajoute des fonds sur un compte.
     */
    AccountDto creditAccount(String accountRef, BigDecimal amount, String userId);

    /**
     * Retire des fonds d'un compte.
     */
    AccountDto debitAccount(String accountRef, BigDecimal amount, String userId);

    /**
     * Effectue un virement entre deux comptes (avec conversion de devise si nécessaire).
     */
    TransactionDto processPayment(PaymentRequestDto request, String userId);

    StatementResponseDto generateStatement(
            String accountRef,
            LocalDate startDate,
            LocalDate endDate,
            String userId
    );

    byte[] exportStatementPdf(String accountRef, LocalDate startDate, LocalDate endDate, String userId);
    byte[] exportStatementCsv(String accountRef, LocalDate startDate, LocalDate endDate, String userId);

}