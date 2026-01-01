package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.Devise;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortefeuillesDTO {
    private Long id;
    private Long utilisateurId;
    private Double balance;
    private Devise devise; // USD, MAD, EUR
//    private List<TransactionDTO> transactions_sortantes;
//    private List<TransactionDTO> transactions_entrantes;
}
