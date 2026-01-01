package com.example.wallet_service.dto;


import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ExpenseResponseDto {
    private BigDecimal amount;
    private String category;
    private String description;
    private LocalDateTime date;
}
