package com.example.payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les événements d'audit publiés dans Kafka
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventDTO {
    
    private String correlationId;
    private String userId;
    private String actionType; // PAYMENT_CREATED, PAYMENT_VALIDATED, PAYMENT_REJECTED
    private String serviceName; // payment-service
    private String description;
    private String details; // JSON avec les détails de la transaction
    private String status; // SUCCESS, FAILURE, ERROR
    private String errorMessage;
    private LocalDateTime timestamp;
    
    // Champs spécifiques au Payment
    private Long paymentId;
    private String sourceAccountId;
    private String destinationIban;
    private Double amount;
    private String transactionType;
    private String transactionStatus;
}

