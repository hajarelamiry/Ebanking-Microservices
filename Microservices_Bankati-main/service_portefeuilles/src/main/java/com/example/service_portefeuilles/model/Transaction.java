package com.example.service_portefeuilles.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "destinateur_id")
    private Portefeuille destinateur;

    @ManyToOne
    @JoinColumn(name = "destinataire_id")
    private Portefeuille destinataire;

    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED

    @Column(nullable = false)
    private LocalDateTime date = LocalDateTime.now();



}

