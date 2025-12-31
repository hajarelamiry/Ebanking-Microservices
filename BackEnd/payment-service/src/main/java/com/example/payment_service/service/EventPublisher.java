package com.example.payment_service.service;

import com.example.payment_service.model.OutboxEvent;
import com.example.payment_service.repository.OutboxEventRepository;
import com.example.payment_service.util.CorrelationIdContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service pour publier des événements dans la table outbox (Transactional Outbox Pattern)
 * Les événements sont enregistrés dans la même transaction que l'action métier
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Publie un événement dans la table outbox
     * Cette méthode doit être appelée dans la même transaction que l'action métier
     * 
     * @param aggregateType Type de l'agrégat (ex: "Payment")
     * @param aggregateId ID de l'agrégat
     * @param eventType Type de l'événement (ex: "PAYMENT_CREATED")
     * @param eventPayload Objet à sérialiser en JSON
     */
    @Transactional
    public void publishEvent(String aggregateType, String aggregateId, String eventType, Object eventPayload) {
        try {
            // Sérialise le payload en JSON
            String payloadJson = objectMapper.writeValueAsString(eventPayload);
            
            // Récupère le Correlation ID depuis le contexte
            String correlationId = CorrelationIdContext.getCorrelationId();
            
            // Crée l'événement outbox
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType(aggregateType)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .payload(payloadJson)
                    .correlationId(correlationId)
                    .status(OutboxEvent.OutboxStatus.PENDING)
                    .build();
            
            // Enregistre dans la table outbox (dans la même transaction)
            outboxEventRepository.save(outboxEvent);
            
            log.info("Event published to outbox: type={}, aggregateId={}, correlationId={}", 
                    eventType, aggregateId, correlationId);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event payload", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}

