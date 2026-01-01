package com.example.service_portefeuilles.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.enums.Devise;

@Data
@AllArgsConstructor
public class SoldeResponseDto {
    private Double solde;
    private Devise devise;
}
