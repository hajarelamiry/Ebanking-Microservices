package org.example.dto;


import lombok.Data;
import org.example.enums.Devise;

import java.time.LocalDate;

@Data
public class CarteVirtuelleDTO {
    private Long id;
    private Long utilisateurId;
    private String numero_carte;
    private String cvv;
    private LocalDate date_expiration;
    private Double limite;

    private Devise devise;
    private String status ;
}

