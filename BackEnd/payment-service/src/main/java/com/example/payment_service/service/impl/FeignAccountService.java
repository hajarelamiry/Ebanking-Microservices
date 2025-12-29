package com.example.payment_service.service.impl;

import com.example.payment_service.client.AccountServiceClient;
import com.example.payment_service.dto.AccountBalanceResponse;
import com.example.payment_service.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implémentation réelle utilisant Feign pour communiquer avec account-service
 * Activée uniquement si payment.mock.enabled=false
 */
@Service
@ConditionalOnProperty(name = "payment.mock.enabled", havingValue = "false", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class FeignAccountService implements AccountService {

    private final AccountServiceClient accountServiceClient;

    @Override
    public AccountBalanceResponse getAccountBalance(UUID accountId) {
        log.debug("Appel réel à account-service pour le solde du compte: {}", accountId);
        return accountServiceClient.getAccountBalance(accountId);
    }

    @Override
    public AccountBalanceResponse debitAccount(UUID accountId, BigDecimal amount, String currency, UUID transactionId) {
        log.debug("Appel réel à account-service pour débit: compte={}, montant={}", accountId, amount);
        AccountServiceClient.DebitRequest request = new AccountServiceClient.DebitRequest(amount, currency, transactionId);
        return accountServiceClient.debitAccount(accountId, request);
    }

    @Override
    public AccountBalanceResponse creditAccount(UUID accountId, BigDecimal amount, String currency, UUID transactionId) {
        log.debug("Appel réel à account-service pour crédit: compte={}, montant={}", accountId, amount);
        AccountServiceClient.CreditRequest request = new AccountServiceClient.CreditRequest(amount, currency, transactionId);
        return accountServiceClient.creditAccount(accountId, request);
    }

    @Override
    public Boolean checkSufficientBalance(UUID accountId, BigDecimal amount) {
        log.debug("Appel réel à account-service pour vérification de solde: compte={}, montant={}", accountId, amount);
        return accountServiceClient.checkSufficientBalance(accountId, amount);
    }
}

