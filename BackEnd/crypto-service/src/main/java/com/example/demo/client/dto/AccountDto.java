package com.example.demo.client.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountDto {
    private String externalReference;   // Référence publique
    private BigDecimal balance;
    private String devise;  // Devise enum as String
    private String status;  // AccountStatus enum as String
    private LocalDateTime createdAt;
}
