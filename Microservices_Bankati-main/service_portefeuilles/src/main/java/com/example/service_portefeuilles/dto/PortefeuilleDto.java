package com.example.service_portefeuilles.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.Devise;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortefeuilleDto {
    private Long id;
    private Long utilisateurId;
    private Double balance;
    private Devise devise;
    private List<ExpenseDTO> expenses;
}
