package com.example.payment_service.repository;

import com.example.payment_service.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour la table outbox_events (Transactional Outbox Pattern)
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Récupère tous les événements en attente de publication
     */
    List<OutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus status);

    /**
     * Récupère les événements en attente avec une limite
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = :status ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEvents(@Param("status") OutboxEvent.OutboxStatus status);

    /**
     * Marque un événement comme publié
     */
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PUBLISHED', e.publishedAt = :publishedAt WHERE e.id = :id")
    void markAsPublished(@Param("id") Long id, @Param("publishedAt") LocalDateTime publishedAt);

    /**
     * Marque un événement comme échoué et incrémente le retry count
     */
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'FAILED', e.retryCount = e.retryCount + 1, e.errorMessage = :errorMessage WHERE e.id = :id")
    void markAsFailed(@Param("id") Long id, @Param("errorMessage") String errorMessage);

    /**
     * Réinitialise le statut d'un événement échoué pour réessayer
     */
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PENDING', e.errorMessage = NULL WHERE e.id = :id AND e.retryCount < :maxRetries")
    void resetForRetry(@Param("id") Long id, @Param("maxRetries") Integer maxRetries);
}

