package com.example.payment_service.service;

import com.example.payment_service.client.AuditClient;
import com.example.payment_service.dto.AuditEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service pour envoyer des événements d'audit via Feign Client (Eureka)
 * Communication synchrone directe avec l'Audit Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditClient auditClient;

    /**
     * Envoie un événement d'audit à l'Audit Service via Feign Client
     * Appel synchrone : l'événement est enregistré immédiatement
     * 
     * @param auditEvent L'événement d'audit à envoyer
     */
    public void sendAuditEvent(AuditEventDTO auditEvent) {
        try {
            // Convertit le DTO local en DTO compatible avec Audit Service
            com.example.payment_service.client.dto.AuditServiceEventDTO auditServiceDTO = convertToAuditServiceDTO(auditEvent);
            
            // Envoie via Feign Client (Eureka découvre automatiquement audit-service)
            auditClient.sendLog(auditServiceDTO);
            
            log.info("Audit event sent via Feign Client: actionType={}, correlationId={}", 
                    auditEvent.getActionType(), auditEvent.getCorrelationId());
            
        } catch (Exception e) {
            log.error("Failed to send audit event via Feign Client: actionType={}, correlationId={}", 
                    auditEvent.getActionType(), auditEvent.getCorrelationId(), e);
            // Le Circuit Breaker gère automatiquement les erreurs
        }
    }

    /**
     * Convertit le DTO local en DTO compatible avec Audit Service
     */
    private com.example.payment_service.client.dto.AuditServiceEventDTO convertToAuditServiceDTO(AuditEventDTO localDTO) {
        return com.example.payment_service.client.dto.AuditServiceEventDTO.builder()
                .userId(localDTO.getUserId())
                .actionType(localDTO.getActionType())
                .serviceName(localDTO.getServiceName())
                .description(localDTO.getDescription())
                .status(localDTO.getStatus())
                .errorMessage(localDTO.getErrorMessage())
                .correlationId(localDTO.getCorrelationId())
                .details(localDTO.getDetails())
                .build();
    }
}

