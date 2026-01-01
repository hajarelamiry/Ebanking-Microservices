package com.example.wallet_service.account;

import com.example.wallet_service.dto.SoldeResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@FeignClient(name = "account-service")
public interface AccountClient {
    @GetMapping("/api/accounts/{ref}/solde")
    SoldeResponseDto getSolde(@PathVariable String ref, @RequestHeader("Authorization") String token);

    @PostMapping("/api/accounts/{ref}/debit")
    void debitAccount(
            @PathVariable String ref,
            @RequestBody Map<String, BigDecimal> payload,
            @RequestHeader("Authorization") String token
    );
}
