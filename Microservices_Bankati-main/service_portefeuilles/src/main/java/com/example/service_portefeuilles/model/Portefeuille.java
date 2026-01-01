package com.example.service_portefeuilles.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entites.Transaction;
import org.example.entites.Utilisateur;
import org.example.enums.Devise;

import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Portefeuille{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private Long utilisateurId;

    @Column(nullable = false)
    private Double balance;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Devise devise;

//    @OneToMany(mappedBy = "destinateur", cascade = CascadeType.ALL , fetch = FetchType.LAZY)
//    private List<Transaction> Transactions_sortantes;
//
//    @OneToMany(mappedBy = "destinataire", cascade = CascadeType.ALL , fetch = FetchType.LAZY)
//    private List<Transaction> Transactions_entrantes;

    @ElementCollection
    @CollectionTable(name = "portefeuille_expenses", joinColumns = @JoinColumn(name = "portefeuille_id"))
    @Column(name = "expense_id")
    private List<Long> expensIds;

}
