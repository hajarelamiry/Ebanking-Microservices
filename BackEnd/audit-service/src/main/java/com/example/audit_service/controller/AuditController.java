package com.example.audit_service.controller;

import com.example.audit_service.dto.AuditEventDTO;
import com.example.audit_service.model.AuditLog;
import com.example.audit_service.service.AuditService;
import com.example.audit_service.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * 1️⃣ & 2️⃣ Journaliser les actions critiques / Centraliser les audits
     * Endpoint pour recevoir les événements d'audit
     */
    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> logEvent(
            @Valid @RequestBody AuditEventDTO eventDTO,
            HttpServletRequest request
    ) {
        AuditLog savedLog = auditService.logEvent(eventDTO, request);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Audit event logged successfully");
        response.put("auditLogId", savedLog.getId());
        response.put("timestamp", savedLog.getTimestamp());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 2️⃣ Centraliser les audits des microservices
     * Endpoint dédié pour les événements depuis d'autres services
     */
    @PostMapping("/events/external")
    public ResponseEntity<Map<String, Object>> receiveExternalEvent(
            @Valid @RequestBody AuditEventDTO eventDTO
    ) {
        AuditLog savedLog = auditService.receiveEventFromService(eventDTO);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "External audit event received and logged");
        response.put("auditLogId", savedLog.getId());
        response.put("timestamp", savedLog.getTimestamp());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 3️⃣ Historique par utilisateur
     * Consulter toutes les actions d'un utilisateur donné
     */
    @GetMapping("/users/{userId}/history")
    public ResponseEntity<Map<String, Object>> getUserHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        // Vérifier que le CLIENT ne peut accéder qu'à son propre historique
        // Spring Security garantit que l'utilisateur est authentifié à ce point
        String currentUserId = JwtUtils.getUserId();
        if (currentUserId != null && JwtUtils.isClient() && !userId.equals(currentUserId)) {
            throw new AccessDeniedException("CLIENT can only access their own history");
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        Page<AuditLog> auditLogs;
        if (actionType != null || status != null || startDate != null || endDate != null) {
            auditLogs = auditService.getUserHistoryWithFilters(
                    userId, actionType, status, startDate, endDate, pageable
            );
        } else {
            auditLogs = auditService.getUserHistory(userId, pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("totalElements", auditLogs.getTotalElements());
        response.put("totalPages", auditLogs.getTotalPages());
        response.put("currentPage", auditLogs.getNumber());
        response.put("auditLogs", auditLogs.getContent());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 4️⃣ Historique global (admin)
     * Vue complète de toutes les actions du système
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getAllHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        Page<AuditLog> auditLogs;
        // Si seul serviceName est fourni, utiliser la méthode dédiée (plus simple et fiable)
        if (serviceName != null && userId == null && actionType == null && 
            status == null && startDate == null && endDate == null) {
            auditLogs = auditService.getHistoryByServiceName(serviceName, pageable);
        } else if (serviceName != null && status != null && userId == null && 
                   actionType == null && startDate == null && endDate == null) {
            // Si serviceName + status uniquement, utiliser la méthode dédiée
            auditLogs = auditService.getHistoryByServiceNameAndStatus(serviceName, status, pageable);
        } else if (userId != null || actionType != null || serviceName != null || 
                   status != null || startDate != null || endDate != null) {
            auditLogs = auditService.getAllHistoryWithFilters(
                    userId, actionType, serviceName, status, startDate, endDate, pageable
            );
        } else {
            auditLogs = auditService.getAllHistory(pageable);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalElements", auditLogs.getTotalElements());
        response.put("totalPages", auditLogs.getTotalPages());
        response.put("currentPage", auditLogs.getNumber());
        response.put("auditLogs", auditLogs.getContent());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 5️⃣ Traçabilité des erreurs et échecs
     * Enregistrer les tentatives échouées et incidents
     */
    @GetMapping("/errors")
    public ResponseEntity<Map<String, Object>> getErrorsAndFailures(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditLogs = auditService.getErrorsAndFailures(pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalElements", auditLogs.getTotalElements());
        response.put("totalPages", auditLogs.getTotalPages());
        response.put("currentPage", auditLogs.getNumber());
        response.put("auditLogs", auditLogs.getContent());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Statistiques
     */
    @GetMapping("/stats/user/{userId}")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable String userId) {
        // Vérifier que le CLIENT ne peut accéder qu'à ses propres stats
        // Spring Security garantit que l'utilisateur est authentifié à ce point
        String currentUserId = JwtUtils.getUserId();
        if (JwtUtils.isClient() && currentUserId != null && !userId.equals(currentUserId)) {
            throw new AccessDeniedException("CLIENT can only access their own stats");
        }
        
        Long actionCount = auditService.getUserActionCount(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("totalActions", actionCount);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/errors")
    public ResponseEntity<Map<String, Object>> getErrorStats() {
        Long errorCount = auditService.getErrorCount();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalErrors", errorCount);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint pour recevoir les logs via Feign Client (Eureka)
     * Utilisé par Payment Service et Crypto Service
     */
    @PostMapping("/log")
    public ResponseEntity<Map<String, Object>> receiveLogViaFeign(
            @Valid @RequestBody AuditEventDTO eventDTO
    ) {
        AuditLog savedLog = auditService.receiveEventFromService(eventDTO);
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Audit log received via Feign Client");
        response.put("auditLogId", savedLog.getId());
        response.put("timestamp", savedLog.getTimestamp());
        response.put("correlationId", savedLog.getCorrelationId());
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "audit-service");
        return ResponseEntity.ok(response);
    }
}

