package com.example.account_service.controller;

import com.example.account_service.dto.*;
import com.example.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Méthode utilitaire pour extraire l'ID utilisateur du token JWT.
     * On récupère le "subject" du token via authentication.getName().
     */
    private Long getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("Utilisateur non authentifié");
        }
        return Long.parseLong(authentication.getName());
    }

    // CRÉER UN NOUVEAU COMPTE (ex: compte en EUR, USD, etc.)
    @PostMapping
    public ResponseEntity<AccountDto> createAccount(
            @RequestBody CreateAccountRequestDto request,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);
        AccountDto response = accountService.createAccount(request, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // CONSULTER LE SOLDE D'UN COMPTE SPÉCIFIQUE
    @GetMapping("/{accountRef}/balance")
    public ResponseEntity<SoldeResponseDto> getBalance(
            @PathVariable String accountRef,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(accountService.consulterSolde(accountRef, userId));
    }

    // DÉPOSER DE L'ARGENT (CRÉDIT)
    @PostMapping("/{accountRef}/credit")
    public ResponseEntity<AccountDto> credit(
            @PathVariable String accountRef,
            @RequestBody Map<String, BigDecimal> payload,
            Authentication authentication) {

        BigDecimal amount = payload.get("amount");
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(accountService.creditAccount(accountRef, amount, userId));
    }

    // RETIRER DE L'ARGENT (DÉBIT)
    @PostMapping("/{accountRef}/debit")
    public ResponseEntity<AccountDto> debit(
            @PathVariable String accountRef,
            @RequestBody Map<String, BigDecimal> payload,
            Authentication authentication) {

        BigDecimal amount = payload.get("amount");
        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(accountService.debitAccount(accountRef, amount, userId));
    }

    // EFFECTUER UN VIREMENT (VERS UN AUTRE COMPTE)
    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transfer(
            @RequestBody PaymentRequestDto request,
            Authentication authentication) {

        Long userId = getAuthenticatedUserId(authentication);
        return ResponseEntity.ok(accountService.processPayment(request, userId));
    }
}