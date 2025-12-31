package com.example.demo.model;

import com.example.demo.enums.CryptoSymbol;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "crypto_wallets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "symbol"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CryptoWallet {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CryptoSymbol symbol;
    
    @Column(nullable = false, columnDefinition = "NUMERIC(19,8)")
    private Double balance;
}

