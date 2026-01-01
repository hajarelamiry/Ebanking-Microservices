package org.example.entites;

import aj.org.objectweb.asm.commons.Remapper;
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
    private Portefeuilles destinateur;

    @ManyToOne
    @JoinColumn(name = "destinataire_id")
    private Portefeuilles destinataire;

    @Column(nullable = false)
    private Double montant;

    @Column(nullable = false)
    private String status; // PENDING, COMPLETED, FAILED

    @Column(nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "carte_virtuelle_id")
    private CarteVirtuelle carteVirtuelle;


}

