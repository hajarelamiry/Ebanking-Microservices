package com.example.demo.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité représentant un événement dans la table outbox (Transactional Outbox Pattern)
 * Cette table stocke les événements à publier dans Kafka dans la même transaction que l'action métier
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status", columnList = "status"),
    @Index(name = "idx_outbox_created", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType; // Ex: "CryptoTransaction"

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId; // ID de l'entité métier (CryptoTransaction.id)

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType; // Ex: "CRYPTO_BUY", "CRYPTO_SELL"

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload; // JSON de l'événement

    @Column(name = "correlation_id", length = 100)
    private String correlationId; // ID de corrélation pour tracer la transaction

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status; // PENDING, PUBLISHED, FAILED

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = OutboxStatus.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    public enum OutboxStatus {
        PENDING,
        PUBLISHED,
        FAILED
    }
}

