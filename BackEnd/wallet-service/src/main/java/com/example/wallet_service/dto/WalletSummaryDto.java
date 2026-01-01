package com.example.wallet_service.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class WalletSummaryDto {
    private String walletName;
    private String walletRef;
    private BigDecimal budgetLimit;
    private BigDecimal totalSpent;
    private BigDecimal remainingBudget;
    private boolean isOverBudget; // Flag visuel pour le front
    private List<ExpenseResponseDto> expenses;
}