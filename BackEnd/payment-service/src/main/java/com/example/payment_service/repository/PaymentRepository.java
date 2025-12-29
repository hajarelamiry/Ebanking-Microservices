package com.example.payment_service.repository;

import com.example.payment_service.model.Payment;
import com.example.payment_service.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour l'entité Payment
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Trouve toutes les transactions par compte source
     */
    List<Payment> findBySourceAccountId(String sourceAccountId);

    /**
     * Trouve toutes les transactions par statut
     */
    List<Payment> findByStatus(TransactionStatus status);

    /**
     * Trouve toutes les transactions par compte source et statut
     */
    List<Payment> findBySourceAccountIdAndStatus(String sourceAccountId, TransactionStatus status);

    /**
     * Compte le nombre de transactions pour un compte source dans une période donnée
     */
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.sourceAccountId = :sourceAccountId AND p.createdAt >= :startTime")
    long countBySourceAccountIdAndCreatedAtAfter(@Param("sourceAccountId") String sourceAccountId, 
                                                   @Param("startTime") LocalDateTime startTime);

    /**
     * Vérifie si un IBAN a déjà été utilisé par un compte source
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.sourceAccountId = :sourceAccountId AND p.destinationIban = :destinationIban")
    boolean existsBySourceAccountIdAndDestinationIban(@Param("sourceAccountId") String sourceAccountId, 
                                                        @Param("destinationIban") String destinationIban);

    /**
     * Calcule la somme des montants des transactions pour un compte source depuis une date donnée
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.sourceAccountId = :sourceAccountId AND p.createdAt >= :startTime")
    Double sumAmountBySourceAccountIdAndCreatedAtAfter(@Param("sourceAccountId") String sourceAccountId, 
                                                        @Param("startTime") LocalDateTime startTime);
}

