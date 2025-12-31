package com.example.demo.model;

import com.example.demo.enums.CryptoSymbol;
import com.example.demo.enums.TradeType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "crypto_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CryptoTransaction {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CryptoSymbol symbol;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;
    
    @Column(nullable = false, columnDefinition = "NUMERIC(19,8)")
    private Double quantity;
    
    @Column(name = "price_at_time", nullable = false, columnDefinition = "NUMERIC(19,2)")
    private Double priceAtTime;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}

