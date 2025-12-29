package com.example.payment_service.service.impl;

import com.example.payment_service.dto.AccountBalanceResponse;
import com.example.payment_service.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Implémentation MOCK pour le développement isolé
 * Simule les opérations sur les comptes sans appeler le service réel
 * 
 * Activée par défaut ou si payment.mock.enabled=true
 */
@Service
@ConditionalOnProperty(name = "payment.mock.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockAccountService implements AccountService {

    // Solde fictif par défaut pour les tests
    private static final BigDecimal DEFAULT_BALANCE = new BigDecimal("10000.00");
    private BigDecimal currentBalance = DEFAULT_BALANCE;

    @Override
    public AccountBalanceResponse getAccountBalance(UUID accountId) {
        log.info("🔵 [MOCK] Vérification du solde pour le compte: {}", accountId);
        
        AccountBalanceResponse response = AccountBalanceResponse.builder()
                .accountId(accountId)
                .balance(currentBalance)
                .currency("EUR")
                .sufficientFunds(currentBalance.compareTo(BigDecimal.ZERO) >= 0)
                .build();
        
        log.info("🔵 [MOCK] Solde simulé: {} {}", response.getBalance(), response.getCurrency());
        return response;
    }

    @Override
    public AccountBalanceResponse debitAccount(UUID accountId, BigDecimal amount, String currency, UUID transactionId) {
        log.info("🔵 [MOCK] Débit simulé pour le compte: {} - Montant: {} {} - Transaction: {}", 
                accountId, amount, currency, transactionId);
        
        // Simulation : débit réussi
        currentBalance = currentBalance.subtract(amount);
        
        AccountBalanceResponse response = AccountBalanceResponse.builder()
                .accountId(accountId)
                .balance(currentBalance)
                .currency(currency)
                .sufficientFunds(currentBalance.compareTo(BigDecimal.ZERO) >= 0)
                .build();
        
        log.info("🔵 [MOCK] Débit effectué. Nouveau solde simulé: {} {}", 
                response.getBalance(), response.getCurrency());
        return response;
    }

    @Override
    public AccountBalanceResponse creditAccount(UUID accountId, BigDecimal amount, String currency, UUID transactionId) {
        log.info("🔵 [MOCK] Crédit simulé pour le compte: {} - Montant: {} {} - Transaction: {}", 
                accountId, amount, currency, transactionId);
        
        // Simulation : crédit réussi
        currentBalance = currentBalance.add(amount);
        
        AccountBalanceResponse response = AccountBalanceResponse.builder()
                .accountId(accountId)
                .balance(currentBalance)
                .currency(currency)
                .sufficientFunds(true)
                .build();
        
        log.info("🔵 [MOCK] Crédit effectué. Nouveau solde simulé: {} {}", 
                response.getBalance(), response.getCurrency());
        return response;
    }

    @Override
    public Boolean checkSufficientBalance(UUID accountId, BigDecimal amount) {
        log.info("🔵 [MOCK] Vérification du solde suffisant pour le compte: {} - Montant: {}", 
                accountId, amount);
        
        boolean sufficient = currentBalance.compareTo(amount) >= 0;
        log.info("🔵 [MOCK] Solde suffisant: {}", sufficient);
        return sufficient;
    }
}

