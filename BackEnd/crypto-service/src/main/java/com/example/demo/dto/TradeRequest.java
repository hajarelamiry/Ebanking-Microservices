package com.example.demo.dto;

import com.example.demo.enums.CryptoSymbol;
import com.example.demo.enums.TradeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeRequest {
    
    @NotNull(message = "Symbol is required")
    private CryptoSymbol symbol;
    
    @NotNull(message = "Type is required")
    private TradeType type;
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Double quantity;
}

