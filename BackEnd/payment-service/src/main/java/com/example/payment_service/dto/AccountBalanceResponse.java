package com.example.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO pour la réponse de solde de compte
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalanceResponse {

    private UUID accountId;
    private BigDecimal balance;
    private String currency;
    private Boolean sufficientFunds;
}

