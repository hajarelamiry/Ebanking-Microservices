package com.example.demo.client;

import com.example.demo.client.dto.AuditServiceEventDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign Client pour communiquer avec l'Audit Service via Eureka
 * Eureka découvre automatiquement l'instance d'audit-service
 */
@FeignClient(name = "audit-service", fallback = AuditClientFallback.class)
public interface AuditClient {

    /**
     * Envoie un événement d'audit à l'Audit Service
     * 
     * @param auditEvent L'événement d'audit à enregistrer
     */
    @PostMapping("/api/audit/log")
    @CircuitBreaker(name = "auditService", fallbackMethod = "fallbackSendLog")
    void sendLog(@RequestBody AuditServiceEventDTO auditEvent);

    /**
     * Méthode de fallback si l'Audit Service est indisponible
     */
    default void fallbackSendLog(AuditServiceEventDTO auditEvent, Exception e) {
        System.err.println("Audit Service unavailable. Event not sent: " + auditEvent.getActionType());
    }
}

