package com.example.audit_service.consumer;

import com.example.audit_service.dto.AuditEventDTO;
import com.example.audit_service.model.AuditLog;
import com.example.audit_service.repository.AuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer Kafka pour consommer les événements d'audit depuis le topic audit-events
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditRepository auditRepository;
    private final ObjectMapper objectMapper;

    /**
     * Consomme les événements d'audit depuis Kafka
     * 
     * @param payload Le payload JSON de l'événement
     * @param correlationId Le correlation ID depuis la clé Kafka
     * @param acknowledgment L'acknowledgment pour confirmer la consommation
     */
    @KafkaListener(
            topics = "audit-events",
            groupId = "audit-service-group",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeAuditEvent(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String correlationId,
            Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received audit event: correlationId={}, payload={}", correlationId, payload);
            
            // Désérialise le payload JSON en AuditEventDTO
            AuditEventDTO eventDTO = objectMapper.readValue(payload, AuditEventDTO.class);
            
            // S'assure que le correlationId est défini
            if (eventDTO.getCorrelationId() == null || eventDTO.getCorrelationId().trim().isEmpty()) {
                eventDTO.setCorrelationId(correlationId);
            }
            
            // Convertit le DTO en entité AuditLog
            AuditLog auditLog = convertDTOToEntity(eventDTO);
            
            // Enregistre dans la base de données
            auditRepository.save(auditLog);
            
            log.info("Audit event saved: id={}, correlationId={}, actionType={}, serviceName={}", 
                    auditLog.getId(), auditLog.getCorrelationId(), 
                    auditLog.getActionType(), auditLog.getServiceName());
            
            // Confirme la consommation du message
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Failed to process audit event: correlationId={}, payload={}", 
                    correlationId, payload, e);
            // En cas d'erreur, on n'acknowledge pas le message pour qu'il soit rejoué
            // Dans un système de production, on pourrait utiliser un Dead Letter Queue
            throw new RuntimeException("Failed to process audit event", e);
        }
    }

    /**
     * Convertit un AuditEventDTO en AuditLog
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
        // Le timestamp sera défini par @PrePersist si null
        return auditLog;
    }
}

