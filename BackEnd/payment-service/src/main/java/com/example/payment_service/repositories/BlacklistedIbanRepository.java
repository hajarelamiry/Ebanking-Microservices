package com.example.payment_service.repositories;

import com.example.payment_service.model.BlacklistedIban;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'entité BlacklistedIban
 */
@Repository
public interface BlacklistedIbanRepository extends JpaRepository<BlacklistedIban, UUID> {

    /**
     * Trouve un IBAN dans la liste noire par son IBAN
     */
    Optional<BlacklistedIban> findByIbanAndIsActiveTrue(String iban);

    /**
     * Vérifie si un IBAN est dans la liste noire
     */
    boolean existsByIbanAndIsActiveTrue(String iban);
}

