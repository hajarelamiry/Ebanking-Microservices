package com.example.service_portefeuilles.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlimentationExpenseRequest {
    private Long portefeuilleId;
    private Long expenseId;
    private Double montant;
}
