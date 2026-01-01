package com.example.wallet_service.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
@Builder
public class WalletResponseDto {
    private String walletRef;
    private String name;
    private String accountRef;
    private BigDecimal budgetLimit;
    private String userId;
}