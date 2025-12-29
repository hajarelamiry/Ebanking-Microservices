package com.example.payment_service.dto;

import com.example.payment_service.enums.TransactionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour la requête de virement
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestDTO {

    @NotBlank(message = "L'ID du compte source est requis")
    @Size(max = 50, message = "L'ID du compte source ne peut pas dépasser 50 caractères")
    private String sourceAccountId;

    @NotBlank(message = "L'IBAN de destination est requis")
    @Size(max = 34, message = "L'IBAN ne peut pas dépasser 34 caractères")
    private String destinationIban;

    @NotNull(message = "Le montant est requis")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private Double amount;

    @NotNull(message = "Le type de transaction est requis")
    private TransactionType type;
}

