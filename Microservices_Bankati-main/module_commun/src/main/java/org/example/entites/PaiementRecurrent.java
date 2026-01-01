package org.example.entites;


import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.example.enums.Devise;
import org.example.enums.Fournisseur;

import java.time.LocalDate;

@Data
@Entity
@Table(name = "paiements_recurrents")
public class PaiementRecurrent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Fournisseur fournisseur;
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String paymentMethod; // Carte virtuelle ou Portefeuille

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Devise currency;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String frequency; // DAILY, WEEKLY, MONTHLY, YEARLY

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate nextExecutionDate;

    @Column(nullable = false)
    private String status; // ACTIVE, CANCELED
}

