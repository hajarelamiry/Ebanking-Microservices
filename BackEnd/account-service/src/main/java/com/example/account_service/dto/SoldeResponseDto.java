package com.example.account_service.dto;

import com.example.account_service.enums.Devise;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SoldeResponseDto {

    private BigDecimal balance;
    private Devise devise;
}
