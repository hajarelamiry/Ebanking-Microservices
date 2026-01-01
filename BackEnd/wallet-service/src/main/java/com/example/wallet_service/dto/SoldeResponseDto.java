package com.example.wallet_service.dto;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoldeResponseDto {

    // Le solde actuel du compte
    private BigDecimal balance;

    // La devise (EUR, USD, MAD, etc.)
    private Devise devise;

    // Optionnel : On peut ajouter la référence pour confirmation
    private String accountRef;
}