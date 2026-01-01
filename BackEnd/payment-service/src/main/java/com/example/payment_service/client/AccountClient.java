package com.example.payment_service.client;

import com.example.payment_service.client.dto.AccountDto;
import com.example.payment_service.client.dto.BalanceResponseDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Feign Client pour communiquer avec l'Account Service via Eureka
 * Eureka découvre automatiquement l'instance d'account-service
 */
@FeignClient(name = "account-service", fallback = AccountClientFallback.class)
public interface AccountClient {

    /**
     * Récupère le solde d'un compte
     * 
     * @param accountRef La référence du compte (externalReference)
     * @return Le solde du compte avec la devise
     */
    @GetMapping("/api/accounts/{accountRef}/balance")
    @CircuitBreaker(name = "accountService", fallbackMethod = "fallbackGetBalance")
    BalanceResponseDTO getBalance(@PathVariable("accountRef") String accountRef);

    /**
     * Débite un compte
     * 
     * @param accountRef La référence du compte
     * @param payload Contient le montant à débiter
     * @return Les informations du compte mis à jour
     */
    @PostMapping("/api/accounts/{accountRef}/debit")
    @CircuitBreaker(name = "accountService", fallbackMethod = "fallbackDebit")
    Map<String, Object> debit(@PathVariable("accountRef") String accountRef, 
                              @RequestBody Map<String, BigDecimal> payload);

    /**
     * Récupère le compte principal d'un utilisateur par son userId
     * 
     * @param userId L'identifiant de l'utilisateur
     * @return Le compte principal de l'utilisateur
     */
    @GetMapping("/api/accounts/user/{userId}")
    @CircuitBreaker(name = "accountService", fallbackMethod = "fallbackGetAccountByUserId")
    AccountDto getAccountByUserId(@PathVariable("userId") String userId);

    /**
     * Méthodes de fallback
     */
    default BalanceResponseDTO fallbackGetBalance(String accountRef, Exception e) {
        throw new RuntimeException("Account Service unavailable. Cannot check balance for account: " + accountRef, e);
    }

    default Map<String, Object> fallbackDebit(String accountRef, Map<String, BigDecimal> payload, Exception e) {
        throw new RuntimeException("Account Service unavailable. Cannot debit account: " + accountRef, e);
    }

    default AccountDto fallbackGetAccountByUserId(String userId, Exception e) {
        throw new RuntimeException("Account Service unavailable. Cannot get account for user: " + userId, e);
    }
}
