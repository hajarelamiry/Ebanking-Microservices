package com.example.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO pour la réponse du legacy-adapter-service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegacyPaymentResponse {

    private UUID transactionId;
    private Boolean success;
    private String message;
    private String legacyReference;
}

