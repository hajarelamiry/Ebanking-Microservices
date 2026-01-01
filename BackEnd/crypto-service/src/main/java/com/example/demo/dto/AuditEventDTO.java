package com.example.demo.dto;

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
    private String actionType; // CRYPTO_BUY, CRYPTO_SELL
    private String serviceName; // crypto-service
    private String description;
    private String details; // JSON avec les détails de la transaction
    private String status; // SUCCESS, FAILURE, ERROR
    private String errorMessage;
    private LocalDateTime timestamp;
    
    // Champs spécifiques au Crypto
    private Long transactionId;
    private String symbol;
    private String tradeType; // BUY, SELL
    private Double quantity;
    private Double priceAtTime;
}

