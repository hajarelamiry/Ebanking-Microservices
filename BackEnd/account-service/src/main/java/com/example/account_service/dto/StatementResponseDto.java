package com.example.account_service.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatementResponseDto {

    private String statementNumber; // RELEVE-2025-000012
    private String accountRef;

    private LocalDate startDate;
    private LocalDate endDate;

    private List<TransactionDto> transactions;

    private int totalTransactions;
}
