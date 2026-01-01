package com.example.cmi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entites.Portefeuilles;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alimentation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Double montant;
    @ManyToOne
    @JoinColumn(name = "compte_destinateur", nullable = false)
    private CompteBancaire destinateur;

    @ManyToOne
    @JoinColumn(name = "portefeuille_destinataire", nullable = false)
    private Portefeuilles destinataire;
    @Column(nullable = false)
    private LocalDateTime date = LocalDateTime.now();

}
