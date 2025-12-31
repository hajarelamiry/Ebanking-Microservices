package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletResponse {
    private Long userId;
    private List<WalletItem> wallets;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WalletItem {
        private String symbol;
        private Double balance;
    }
}

