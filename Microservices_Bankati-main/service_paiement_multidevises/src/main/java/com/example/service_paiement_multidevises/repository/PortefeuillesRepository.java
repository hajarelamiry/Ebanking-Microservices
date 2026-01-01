package com.example.service_paiement_multidevises.repository;

import org.example.entites.Portefeuilles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PortefeuillesRepository extends JpaRepository<Portefeuilles , Long> {
    // Trouver le balance par ID du portefeuille
    @Query("SELECT p.balance FROM Portefeuilles p WHERE p.id = :id")
    Double findBalanceById(@Param("id") Long id);
}
