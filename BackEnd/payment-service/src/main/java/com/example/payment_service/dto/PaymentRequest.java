package com.example.payment_service.dto;

import com.example.payment_service.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO pour la requête de virement
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {

    @NotNull(message = "L'ID du compte source est requis")
    private UUID sourceAccountId;

    @NotBlank(message = "L'IBAN de destination est requis")
    @Size(max = 34, message = "L'IBAN ne peut pas dépasser 34 caractères")
    private String destinationIban;

    @NotNull(message = "Le montant est requis")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    @Digits(integer = 17, fraction = 2, message = "Le montant doit avoir au maximum 17 chiffres avant la virgule et 2 après")
    private BigDecimal amount;

    @NotBlank(message = "La devise est requise")
    @Size(min = 3, max = 3, message = "La devise doit être un code ISO à 3 lettres")
    private String currency;

    @NotNull(message = "Le type de transaction est requis")
    private TransactionType type;
}

