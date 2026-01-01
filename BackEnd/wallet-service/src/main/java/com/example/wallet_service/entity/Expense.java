package com.example.wallet_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;
    private String category;
    private String description;
    private LocalDateTime date;

    @PrePersist
    public void init() {
        this.date = LocalDateTime.now();
    }
}