package org.example.entites;

import jakarta.persistence.*;
import lombok.Data;
import org.example.enums.Devise;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.util.List;

@Data
@Entity
@Table(name = "cartes_virtuelles")
public class CarteVirtuelle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false, unique = true)
    private String numero_carte;

    @Column(nullable = false)
    private String cvv;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Devise devise;

    @Column(nullable = false)
    private LocalDate date_expiration;

    @Column(nullable = false)
    private Double limite;

    @Value("${status:Active}")
    private String status;

    @OneToMany(mappedBy = "carteVirtuelle", cascade = CascadeType.ALL , fetch = FetchType.LAZY)
    private List<Transaction> Transactions_sortantes;

    public CarteVirtuelle() {
        this.status = "Active";
    }



}
