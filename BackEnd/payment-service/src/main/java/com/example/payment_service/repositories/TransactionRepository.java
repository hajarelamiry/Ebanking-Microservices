package com.example.payment_service.repositories;

import com.example.payment_service.model.Transaction;
import com.example.payment_service.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository pour l'entité Transaction
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    /**
     * Trouve toutes les transactions par compte source
     */
    List<Transaction> findBySourceAccountId(UUID sourceAccountId);

    /**
     * Trouve toutes les transactions par statut
     */
    List<Transaction> findByStatus(TransactionStatus status);

    /**
     * Trouve toutes les transactions par compte source et statut
     */
    List<Transaction> findBySourceAccountIdAndStatus(UUID sourceAccountId, TransactionStatus status);

    /**
     * Trouve toutes les transactions par compte source et liste de statuts
     */
    List<Transaction> findBySourceAccountIdAndStatusIn(UUID sourceAccountId, List<TransactionStatus> statuses);
}

