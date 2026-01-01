package com.example.account_service.controller;

import com.example.account_service.dto.*;
import com.example.account_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;


import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    private String getAuthenticatedUsername(Jwt jwt) {
        return jwt.getClaimAsString("preferred_username");
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(
            @RequestBody CreateAccountRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {

        String username = getAuthenticatedUsername(jwt);
        AccountDto response = accountService.createAccount(request, username);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{accountRef}/balance")
    public ResponseEntity<SoldeResponseDto> getBalance(
            @PathVariable String accountRef,
            @AuthenticationPrincipal Jwt jwt) {

        String username = getAuthenticatedUsername(jwt);
        return ResponseEntity.ok(accountService.consulterSolde(accountRef, username));
    }

    @PostMapping("/{accountRef}/credit")
    public ResponseEntity<AccountDto> credit(
            @PathVariable String accountRef,
            @RequestBody Map<String, BigDecimal> payload,
            @AuthenticationPrincipal Jwt jwt) {

        BigDecimal amount = payload.get("amount");
        String username = getAuthenticatedUsername(jwt);
        return ResponseEntity.ok(accountService.creditAccount(accountRef, amount, username));
    }

    @PostMapping("/{accountRef}/debit")
    public ResponseEntity<AccountDto> debit(
            @PathVariable String accountRef,
            @RequestBody Map<String, BigDecimal> payload,
            @AuthenticationPrincipal Jwt jwt) {

        BigDecimal amount = payload.get("amount");
        String username = getAuthenticatedUsername(jwt);
        return ResponseEntity.ok(accountService.debitAccount(accountRef, amount, username));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transfer(
            @RequestBody PaymentRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {

        String username = getAuthenticatedUsername(jwt);
        return ResponseEntity.ok(accountService.processPayment(request, username));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<AccountDto> getAccountByUserId(
            @PathVariable String userId,
            @AuthenticationPrincipal Jwt jwt) {

        // Vérifier que l'utilisateur demande son propre compte
        String authenticatedUsername = getAuthenticatedUsername(jwt);
        if (!authenticatedUsername.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            AccountDto account = accountService.getAccountByUserId(userId);
            return ResponseEntity.ok(account);
        } catch (RuntimeException e) {
            // Si aucun compte n'existe, retourner 404 au lieu de 500
            if (e.getMessage() != null && e.getMessage().contains("Aucun compte trouvé")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            // Pour les autres erreurs, laisser Spring gérer (500)
            throw e;
        }
    }
}
