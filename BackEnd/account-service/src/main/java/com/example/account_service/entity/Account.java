package com.example.account_service.entity;

import com.example.account_service.enums.AccountStatus;
import com.example.account_service.enums.Devise;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "accounts",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"utilisateur_id", "devise"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID interne (DB)

    // Référence externe exposée aux APIs (sécurité)
    @Column(name = "external_ref", unique = true, nullable = false, updatable = false, length = 36)
    private String externalReference;

    // Propriétaire du compte (issu du JWT)
    @Column(name = "utilisateur_id", nullable = false, updatable = false)
    private String utilisateurId;

    // Solde sécurisé
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    // Devise du compte
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Devise devise;

    // Statut bancaire
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AccountStatus status; // ACTIF, GELE, CLOS

    // Optimistic locking (anti double débit)
    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void init() {
        this.externalReference = UUID.randomUUID().toString();
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO;
        }
        if (this.status == null) {
            this.status = AccountStatus.ACTIF;
        }
        this.createdAt = LocalDateTime.now();
    }
}

