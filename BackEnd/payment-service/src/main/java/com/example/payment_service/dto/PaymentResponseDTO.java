package com.example.payment_service.dto;

import com.example.payment_service.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour la réponse de virement
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponseDTO {

    private Long id;
    private String sourceAccountId;
    private String destinationIban;
    private Double amount;
    private TransactionStatus status;
    private String message;
    private LocalDateTime createdAt;
}

