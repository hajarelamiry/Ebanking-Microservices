package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO pour les réponses d'erreur
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String error;
    private String errorCode;
    private String message;
    private String service;
    private LocalDateTime timestamp;
    private Object details; // Pour des détails supplémentaires (ex: solde disponible, solde requis)
}
