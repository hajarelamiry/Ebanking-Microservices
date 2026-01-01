package com.example.demo.service;

import com.example.demo.client.AccountClient;
import com.example.demo.client.dto.AccountDto;
import com.example.demo.client.dto.BalanceResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Service pour communiquer avec l'Account Service via Feign Client
 * Gère les opérations bancaires (vérification de solde, débit, crédit) pour les transactions crypto
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    private final AccountClient accountClient;

    /**
     * Vérifie le solde d'un compte
     * 
     * @param accountRef La référence du compte (externalReference)
     * @return Le solde du compte
     * @throws RuntimeException si le service est indisponible ou si le compte n'existe pas
     */
    public BigDecimal getBalance(String accountRef) {
        try {
            BalanceResponseDTO balanceResponse = accountClient.getBalance(accountRef);
            log.info("Balance retrieved for account {}: {} {}", accountRef, balanceResponse.getBalance(), balanceResponse.getDevise());
            return balanceResponse.getBalance();
        } catch (Exception e) {
            log.error("Failed to get balance for account {}: {}", accountRef, e.getMessage());
            throw new RuntimeException("Cannot retrieve account balance: " + e.getMessage(), e);
        }
    }

    /**
     * Vérifie si le solde est suffisant pour une opération
     * 
     * @param accountRef La référence du compte
     * @param requiredAmount Le montant requis
     * @return true si le solde est suffisant, false sinon
     */
    public boolean hasSufficientBalance(String accountRef, BigDecimal requiredAmount) {
        BigDecimal balance = getBalance(accountRef);
        boolean sufficient = balance.compareTo(requiredAmount) >= 0;
        log.info("Balance check for account {}: balance={}, required={}, sufficient={}", 
                accountRef, balance, requiredAmount, sufficient);
        return sufficient;
    }

    /**
     * Débite un compte (pour les achats de crypto)
     * 
     * @param accountRef La référence du compte
     * @param amount Le montant à débiter
     * @throws RuntimeException si le service est indisponible ou si le débit échoue
     */
    public void debitAccount(String accountRef, BigDecimal amount) {
        try {
            Map<String, BigDecimal> payload = new HashMap<>();
            payload.put("amount", amount);
            accountClient.debit(accountRef, payload);
            log.info("Account {} debited with amount: {}", accountRef, amount);
        } catch (Exception e) {
            log.error("Failed to debit account {} with amount {}: {}", accountRef, amount, e.getMessage());
            throw new RuntimeException("Cannot debit account: " + e.getMessage(), e);
        }
    }

    /**
     * Crédite un compte (pour les ventes de crypto)
     * 
     * @param accountRef La référence du compte
     * @param amount Le montant à créditer
     * @throws RuntimeException si le service est indisponible ou si le crédit échoue
     */
    public void creditAccount(String accountRef, BigDecimal amount) {
        try {
            Map<String, BigDecimal> payload = new HashMap<>();
            payload.put("amount", amount);
            accountClient.credit(accountRef, payload);
            log.info("Account {} credited with amount: {}", accountRef, amount);
        } catch (Exception e) {
            log.error("Failed to credit account {} with amount {}: {}", accountRef, amount, e.getMessage());
            throw new RuntimeException("Cannot credit account: " + e.getMessage(), e);
        }
    }

    /**
     * Récupère la référence du compte (accountRef) d'un utilisateur par son userId
     * 
     * @param userId L'identifiant de l'utilisateur (en String)
     * @return La référence du compte (externalReference)
     * @throws RuntimeException si le service est indisponible ou si aucun compte n'est trouvé
     */
    public String getAccountRefByUserId(String userId) {
        try {
            AccountDto account = accountClient.getAccountByUserId(userId);
            log.info("Account reference retrieved for user {}: {}", userId, account.getExternalReference());
            return account.getExternalReference();
        } catch (Exception e) {
            log.error("Failed to get account reference for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Cannot get account reference for user: " + e.getMessage(), e);
        }
    }
}
