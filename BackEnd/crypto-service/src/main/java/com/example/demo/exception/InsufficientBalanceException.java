package com.example.demo.exception;

import java.math.BigDecimal;

/**
 * Exception levée quand le solde est insuffisant
 */
public class InsufficientBalanceException extends BusinessException {
    
    private final BigDecimal required;
    private final BigDecimal available;
    
    public InsufficientBalanceException(String message, BigDecimal required, BigDecimal available) {
        super("INSUFFICIENT_BALANCE", message);
        this.required = required;
        this.available = available;
    }
    
    public BigDecimal getRequired() {
        return required;
    }
    
    public BigDecimal getAvailable() {
        return available;
    }
}
