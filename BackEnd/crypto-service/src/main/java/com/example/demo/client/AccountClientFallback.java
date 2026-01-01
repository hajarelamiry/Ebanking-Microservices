package com.example.demo.client;

import com.example.demo.client.dto.AccountDto;
import com.example.demo.client.dto.BalanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Fallback pour AccountClient en cas d'indisponibilité du service
 */
@Component
@Slf4j
public class AccountClientFallback implements AccountClient {

    @Override
    public BalanceResponseDTO getBalance(String accountRef) {
        log.error("Account Service unavailable. Cannot check balance for account: {}", accountRef);
        throw new RuntimeException("Account Service unavailable. Cannot check balance for account: " + accountRef);
    }

    @Override
    public Map<String, Object> debit(String accountRef, Map<String, BigDecimal> payload) {
        log.error("Account Service unavailable. Cannot debit account: {}", accountRef);
        throw new RuntimeException("Account Service unavailable. Cannot debit account: " + accountRef);
    }

    @Override
    public Map<String, Object> credit(String accountRef, Map<String, BigDecimal> payload) {
        log.error("Account Service unavailable. Cannot credit account: {}", accountRef);
        throw new RuntimeException("Account Service unavailable. Cannot credit account: " + accountRef);
    }

    @Override
    public AccountDto getAccountByUserId(String userId) {
        log.error("Account Service unavailable. Cannot get account for user: {}", userId);
        throw new RuntimeException("Account Service unavailable. Cannot get account for user: " + userId);
    }
}
