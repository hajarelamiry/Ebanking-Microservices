package com.example.payment_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO pour la réponse de solde depuis Account Service
 * Correspond à SoldeResponseDto de account-service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceResponseDTO {
    private BigDecimal balance;
    private String devise; // EUR, USD, etc. (sera sérialisé depuis l'enum Devise)
}
