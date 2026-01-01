package com.example.wallet_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String walletRef; // Externe (UUID)

    private String userId;     // ID de l'utilisateur (via JWT)
    private String accountRef; // Référence vers le compte bancaire (Account-Service)
    private String name;

    @Column(precision = 19, scale = 4)
    private BigDecimal budgetLimit;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Expense> expenses = new ArrayList<>();

    @PrePersist
    public void init() {
        this.walletRef = "WLT-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
