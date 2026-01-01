package com.example.account_service.dto;

import com.example.account_service.enums.Devise;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAccountRequestDto {

    private Devise devise;              // MAD, EUR, USD
    private BigDecimal initialBalance;  // optionnel
}
