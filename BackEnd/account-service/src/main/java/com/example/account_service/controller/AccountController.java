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
import java.time.LocalDate;
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

    @GetMapping("/{accountRef}/statements")
    public ResponseEntity<StatementResponseDto> getStatement(
            @PathVariable String accountRef,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @AuthenticationPrincipal Jwt jwt) {

        String username = getAuthenticatedUsername(jwt);

        return ResponseEntity.ok(
                accountService.generateStatement(
                        accountRef,
                        startDate,
                        endDate,
                        username
                )
        );
    }

    @GetMapping("/{accountRef}/statements/export/csv")
    public ResponseEntity<byte[]> exportStatementCsv(
            @PathVariable String accountRef,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @AuthenticationPrincipal Jwt jwt) {

        String username = getAuthenticatedUsername(jwt);

        byte[] csv = accountService.exportStatementCsv(
                accountRef,
                startDate,
                endDate,
                username
        );

        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header(
                        "Content-Disposition",
                        "attachment; filename=statement-" + accountRef + ".csv"
                )
                .body(csv);
    }


    @GetMapping("/{accountRef}/statements/export/pdf")
    public ResponseEntity<byte[]> exportStatementPdf(
            @PathVariable String accountRef,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @AuthenticationPrincipal Jwt jwt) {

        String username = getAuthenticatedUsername(jwt);

        byte[] pdf = accountService.exportStatementPdf(
                accountRef,
                startDate,
                endDate,
                username
        );

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header(
                        "Content-Disposition",
                        "attachment; filename=releve-" + accountRef + ".pdf"
                )
                .body(pdf);
    }


}
