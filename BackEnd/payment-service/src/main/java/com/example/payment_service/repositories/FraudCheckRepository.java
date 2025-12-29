package com.example.payment_service.repositories;

import com.example.payment_service.model.FraudCheck;
import com.example.payment_service.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour l'entité FraudCheck
 */
@Repository
public interface FraudCheckRepository extends JpaRepository<FraudCheck, UUID> {

    /**
     * Trouve tous les contrôles de fraude pour une transaction donnée (par ID de transaction)
     */
    List<FraudCheck> findByTransaction_Id(UUID transactionId);

    /**
     * Trouve tous les contrôles de fraude pour une transaction
     */
    List<FraudCheck> findByTransaction(Transaction transaction);
}

