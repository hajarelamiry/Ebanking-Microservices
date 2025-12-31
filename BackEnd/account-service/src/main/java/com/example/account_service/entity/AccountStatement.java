package com.example.account_service.entity;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountStatement {

    private Account account;

    private List<Transaction> transactions;

    private Double finalBalance;
}

