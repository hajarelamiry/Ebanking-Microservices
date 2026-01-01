package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.enums.Devise;
import org.example.enums.Fournisseur;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaiementRecurrentDTO {
        private Long id;
        private Long userId;
        private String paymentMethod;
        private Fournisseur fournisseur;
        private Devise currency;
        private Double amount;
        private String frequency; // DAILY, WEEKLY, MONTHLY, YEARLY
        private LocalDate startDate;
        private String status;
        private LocalDate nextExecutionDate;


}
