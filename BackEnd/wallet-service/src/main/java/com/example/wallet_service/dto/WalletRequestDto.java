package com.example.wallet_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRequestDto {

    @NotBlank(message = "Le nom du portefeuille est obligatoire")
    private String name;

    @NotBlank(message = "La référence du compte bancaire est obligatoire")
    private String accountRef; // Le compte source

    @NotNull(message = "La limite de budget est obligatoire")
    @Positive(message = "La limite doit être positive")
    private BigDecimal budgetLimit;
}