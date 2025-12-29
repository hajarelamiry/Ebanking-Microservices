package com.example.payment_service.dto;

import com.example.payment_service.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Événement Saga pour la gestion des transactions distribuées
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaEvent {

    private UUID sagaId;
    private UUID transactionId;
    private String eventType; // TRANSACTION_CREATED, ACCOUNT_DEBITED, LEGACY_PAYMENT_SENT, TRANSACTION_COMPLETED, TRANSACTION_FAILED, COMPENSATION_STARTED
    private TransactionType transactionType;
    private UUID sourceAccountId;
    private String destinationIban;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String errorMessage;
    private LocalDateTime timestamp;
}

