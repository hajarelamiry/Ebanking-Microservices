package com.example.service_portefeuilles.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class TransactionDTO {
    private Long id;
    private Long destinateurId;
    private Long destinataireId;
    private Double montant;
    private String status; // PENDING, COMPLETED, FAILED
    private LocalDateTime date;
}