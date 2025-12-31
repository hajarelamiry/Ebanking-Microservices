package com.example.account_service.entity;

import com.example.account_service.enums.TransactionStatus;
import com.example.account_service.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.codec.digest.DigestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_txn_id", columnList = "transactionId"),
                @Index(name = "idx_txn_date", columnList = "dateHeure")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Référence métier unique (client / audit)
    @Column(nullable = false, unique = true, length = 40, updatable = false)
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinateur_id", nullable = false)
    @JsonIgnore
    private Account destinateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinataire_id", nullable = false)
    @JsonIgnore
    private Account destinataire;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateHeure;

    // Audit & sécurité
    @Column(length = 45)
    private String ipAddress;

    @Column(length = 100)
    private String deviceFingerprint;

    @Column(length = 500)
    private String motif;

    // Optionnel : intégrité de la transaction
    @Column(length = 64)
    private String checksum;

    @PrePersist
    public void onCreate() {
        this.transactionId = "TXN-" + UUID.randomUUID();
        this.dateHeure = LocalDateTime.now();
        this.checksum = generateChecksum();
    }

    private String generateChecksum() {
        return DigestUtils.sha256Hex(
                transactionId +
                        montant +
                        dateHeure
        );
    }
}

