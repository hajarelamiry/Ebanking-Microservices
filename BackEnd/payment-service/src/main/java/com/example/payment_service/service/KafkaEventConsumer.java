package com.example.payment_service.service;

import com.example.payment_service.dto.SagaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Service pour consommer les événements Kafka
 * Note: Si Kafka n'est pas disponible, les événements seront ignorés
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventConsumer {

    private final SagaOrchestrator sagaOrchestrator;

    /**
     * Consomme les événements Saga depuis Kafka
     */
    @KafkaListener(topics = "payment-saga-events", groupId = "payment-service-group")
    public void consumeSagaEvent(SagaEvent event) {
        log.info("Événement Saga reçu: {} pour la transaction {}", 
                event.getEventType(), event.getTransactionId());
        
        try {
            sagaOrchestrator.handleSagaEvent(event);
        } catch (Exception e) {
            log.error("Erreur lors du traitement de l'événement Saga: {}", e.getMessage(), e);
        }
    }
}

