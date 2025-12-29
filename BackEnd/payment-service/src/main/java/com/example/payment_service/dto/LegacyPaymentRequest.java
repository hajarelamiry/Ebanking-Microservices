package com.example.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO pour la requête vers le legacy-adapter-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyPaymentRequest {

    private UUID transactionId;
    private UUID sourceAccountId;
    private String destinationIban;
    private BigDecimal amount;
    private String currency;
}

