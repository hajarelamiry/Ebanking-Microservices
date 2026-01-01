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
public class ExpenseRequestDto {

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être supérieur à zéro")
    private BigDecimal amount;

    @NotBlank(message = "La catégorie est obligatoire")
    private String category; // Ex: ALIMENTATION, LOISIRS...

    private String description;
}