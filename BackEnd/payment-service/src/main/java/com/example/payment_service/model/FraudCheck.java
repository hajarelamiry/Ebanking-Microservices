package com.example.payment_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entité FraudCheck représentant une vérification de fraude pour une transaction
 */
@Entity
@Table(name = "fraud_checks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Column(name = "violated_rule", length = 500)
    private String violatedRule;

    @Column(name = "decision", nullable = false, length = 50)
    private String decision;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

