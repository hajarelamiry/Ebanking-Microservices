package com.example.payment_service.client;

import com.example.payment_service.dto.AccountBalanceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Client Feign pour communiquer avec l'account-service
 * 
 * En mode MOCK, cette interface est désactivée et remplacée par MockAccountServiceClient
 */
@FeignClient(
    name = "account-service",
    url = "${feign.client.account-service.url:http://localhost:8081}",
    path = "/api/accounts"
)
public interface AccountServiceClient {

    /**
     * Vérifie le solde d'un compte
     */
    @GetMapping("/{accountId}/balance")
    AccountBalanceResponse getAccountBalance(@PathVariable UUID accountId);

    /**
     * Débite un compte (pour virement instantané)
     */
    @PostMapping("/{accountId}/debit")
    AccountBalanceResponse debitAccount(
            @PathVariable UUID accountId,
            @RequestBody DebitRequest request
    );

    /**
     * Crédite un compte (pour compensation Saga)
     */
    @PostMapping("/{accountId}/credit")
    AccountBalanceResponse creditAccount(
            @PathVariable UUID accountId,
            @RequestBody CreditRequest request
    );

    /**
     * Vérifie si le solde est suffisant
     */
    @GetMapping("/{accountId}/check-balance/{amount}")
    Boolean checkSufficientBalance(@PathVariable UUID accountId, @PathVariable BigDecimal amount);

    /**
     * DTO pour la requête de débit
     */
    record DebitRequest(BigDecimal amount, String currency, UUID transactionId) {}

    /**
     * DTO pour la requête de crédit
     */
    record CreditRequest(BigDecimal amount, String currency, UUID transactionId) {}
}

