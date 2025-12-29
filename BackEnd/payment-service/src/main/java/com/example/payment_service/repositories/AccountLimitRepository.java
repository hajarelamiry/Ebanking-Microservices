package com.example.payment_service.repositories;

import com.example.payment_service.model.AccountLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository pour l'entité AccountLimit
 */
@Repository
public interface AccountLimitRepository extends JpaRepository<AccountLimit, UUID> {

    /**
     * Trouve les plafonds d'un compte
     */
    Optional<AccountLimit> findByAccountIdAndCurrency(UUID accountId, String currency);
}

