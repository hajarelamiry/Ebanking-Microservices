package com.example.payment_service.client;

import com.example.payment_service.client.dto.AuditServiceEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Fallback pour AuditClient en cas d'indisponibilité de l'Audit Service
 * Le Circuit Breaker active automatiquement ce fallback
 */
@Component
@Slf4j
public class AuditClientFallback implements AuditClient {

    @Override
    public void sendLog(AuditServiceEventDTO auditEvent) {
        log.error("Audit Service is unavailable (Circuit Breaker OPEN). Event not sent: actionType={}, userId={}, correlationId={}", 
                auditEvent.getActionType(), 
                auditEvent.getUserId(),
                auditEvent.getCorrelationId());
        // TODO: Implémenter une queue de secours ou un fichier de log local
        // Les événements sont toujours envoyés via Kafka (Transactional Outbox Pattern)
    }
}

