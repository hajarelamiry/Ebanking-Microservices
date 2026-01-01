package com.example.audit_service.repository;

import com.example.audit_service.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditRepository extends JpaRepository<AuditLog, Long> {

    // Historique par utilisateur
    List<AuditLog> findByUserIdOrderByTimestampDesc(String userId);

    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    // Historique par utilisateur avec filtres
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId " +
           "AND (:actionType IS NULL OR a.actionType = :actionType) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:startDate IS NULL OR a.timestamp >= :startDate) " +
           "AND (:endDate IS NULL OR a.timestamp <= :endDate) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUserIdWithFilters(
            @Param("userId") String userId,
            @Param("actionType") String actionType,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Historique global (admin)
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    // Historique global avec filtres
    @Query("SELECT a FROM AuditLog a WHERE " +
           "(:userId IS NULL OR a.userId = :userId) " +
           "AND (:actionType IS NULL OR a.actionType = :actionType) " +
           "AND (:serviceName IS NULL OR a.serviceName = :serviceName) " +
           "AND (:status IS NULL OR a.status = :status) " +
           "AND (:startDate IS NULL OR a.timestamp >= :startDate) " +
           "AND (:endDate IS NULL OR a.timestamp <= :endDate) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findAllWithFilters(
            @Param("userId") String userId,
            @Param("actionType") String actionType,
            @Param("serviceName") String serviceName,
            @Param("status") String status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // Traçabilité des erreurs et échecs
    List<AuditLog> findByStatusInOrderByTimestampDesc(List<String> statuses);

    Page<AuditLog> findByStatusInOrderByTimestampDesc(List<String> statuses, Pageable pageable);

    // Par service
    List<AuditLog> findByServiceNameOrderByTimestampDesc(String serviceName);

    Page<AuditLog> findByServiceNameOrderByTimestampDesc(String serviceName, Pageable pageable);

    // Par service et status
    Page<AuditLog> findByServiceNameAndStatusOrderByTimestampDesc(String serviceName, String status, Pageable pageable);

    // Par type d'action
    List<AuditLog> findByActionTypeOrderByTimestampDesc(String actionType);

    Page<AuditLog> findByActionTypeOrderByTimestampDesc(String actionType, Pageable pageable);

    // Par correlation ID (pour tracer une transaction complète)
    List<AuditLog> findByCorrelationIdOrderByTimestampAsc(String correlationId);

    Page<AuditLog> findByCorrelationIdOrderByTimestampAsc(String correlationId, Pageable pageable);

    // Statistiques
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId")
    Long countByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.status IN :statuses")
    Long countByStatusIn(@Param("statuses") List<String> statuses);
}

