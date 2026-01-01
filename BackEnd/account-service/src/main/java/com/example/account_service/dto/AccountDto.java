package com.example.account_service.dto;

import com.example.account_service.enums.AccountStatus;
import com.example.account_service.enums.Devise;
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
    private Devise devise;
    private AccountStatus status;
    private LocalDateTime createdAt;
}

