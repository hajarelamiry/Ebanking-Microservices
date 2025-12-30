package com.example.audit_service.service;

import com.example.audit_service.dto.AuditEventDTO;
import com.example.audit_service.model.AuditLog;
import com.example.audit_service.repository.AuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * 1️⃣ Journaliser les actions critiques
     * Enregistre un événement d'audit depuis n'importe quel service
     */
    public AuditLog logEvent(AuditEventDTO eventDTO, HttpServletRequest request) {
        AuditLog auditLog = convertDTOToEntity(eventDTO);
        
        // Extraction des informations de la requête si disponibles
        if (request != null) {
            auditLog.setIpAddress(getClientIpAddress(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
        }
        
        return auditRepository.save(auditLog);
    }

    /**
     * 1️⃣ Journaliser les actions critiques (sans HttpServletRequest)
     */
    public AuditLog logEvent(AuditEventDTO eventDTO) {
        return logEvent(eventDTO, null);
    }

    /**
     * 2️⃣ Centraliser les audits des microservices
     * Méthode dédiée pour recevoir les événements depuis d'autres services
     */
    public AuditLog receiveEventFromService(AuditEventDTO eventDTO) {
        // Validation supplémentaire pour les événements externes
        if (eventDTO.getServiceName() == null || eventDTO.getServiceName().isEmpty()) {
            throw new IllegalArgumentException("Service name is required for external events");
        }
        
        AuditLog auditLog = convertDTOToEntity(eventDTO);
        return auditRepository.save(auditLog);
    }

    /**
     * 3️⃣ Historique par utilisateur
     * Consulter toutes les actions d'un utilisateur donné
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getUserHistory(String userId) {
        return auditRepository.findByUserIdOrderByTimestampDesc(userId);
    }

    /**
     * 3️⃣ Historique par utilisateur avec pagination
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getUserHistory(String userId, Pageable pageable) {
        return auditRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    /**
     * 3️⃣ Historique par utilisateur avec filtres
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getUserHistoryWithFilters(
            String userId,
            String actionType,
            String status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        // Normaliser les chaînes vides en null pour JPQL
        String normalizedActionType = (actionType != null && actionType.trim().isEmpty()) ? null : actionType;
        String normalizedStatus = (status != null && status.trim().isEmpty()) ? null : status;
        
        return auditRepository.findByUserIdWithFilters(
                userId, normalizedActionType, normalizedStatus, startDate, endDate, pageable
        );
    }

    /**
     * 4️⃣ Historique global (admin)
     * Vue complète de toutes les actions du système
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAllHistory(Pageable pageable) {
        return auditRepository.findAllByOrderByTimestampDesc(pageable);
    }

    /**
     * 4️⃣ Historique global avec filtres (admin)
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAllHistoryWithFilters(
            String userId,
            String actionType,
            String serviceName,
            String status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        // Normaliser les chaînes vides en null pour JPQL
        String normalizedUserId = (userId != null && userId.trim().isEmpty()) ? null : userId;
        String normalizedActionType = (actionType != null && actionType.trim().isEmpty()) ? null : actionType;
        String normalizedServiceName = (serviceName != null && serviceName.trim().isEmpty()) ? null : serviceName;
        String normalizedStatus = (status != null && status.trim().isEmpty()) ? null : status;
        
        return auditRepository.findAllWithFilters(
                normalizedUserId, normalizedActionType, normalizedServiceName, normalizedStatus, startDate, endDate, pageable
        );
    }

    /**
     * 5️⃣ Traçabilité des erreurs et échecs
     * Enregistrer les tentatives échouées et incidents
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getErrorsAndFailures() {
        return auditRepository.findByStatusInOrderByTimestampDesc(
                Arrays.asList("FAILURE", "ERROR")
        );
    }

    /**
     * 5️⃣ Traçabilité des erreurs et échecs avec pagination
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getErrorsAndFailures(Pageable pageable) {
        return auditRepository.findByStatusInOrderByTimestampDesc(
                Arrays.asList("FAILURE", "ERROR"), pageable
        );
    }

    /**
     * Méthodes utilitaires
     */
    private AuditLog convertDTOToEntity(AuditEventDTO dto) {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(dto.getUserId());
        auditLog.setActionType(dto.getActionType());
        auditLog.setServiceName(dto.getServiceName());
        auditLog.setDescription(dto.getDescription());
        auditLog.setDetails(dto.getDetails());
        auditLog.setStatus(dto.getStatus());
        auditLog.setErrorMessage(dto.getErrorMessage());
        auditLog.setIpAddress(dto.getIpAddress());
        auditLog.setUserAgent(dto.getUserAgent());
        auditLog.setCorrelationId(dto.getCorrelationId());
        auditLog.setTimestamp(LocalDateTime.now());
        return auditLog;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Statistiques
     */
    @Transactional(readOnly = true)
    public Long getUserActionCount(String userId) {
        return auditRepository.countByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Long getErrorCount() {
        return auditRepository.countByStatusIn(Arrays.asList("FAILURE", "ERROR"));
    }
}

