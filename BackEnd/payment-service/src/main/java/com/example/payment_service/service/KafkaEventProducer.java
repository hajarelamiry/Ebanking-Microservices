package com.example.payment_service.service;

import com.example.payment_service.dto.SagaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service pour publier des événements Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaEventProducer {

    private final KafkaTemplate<String, SagaEvent> kafkaTemplate;
    private static final String SAGA_EVENTS_TOPIC = "payment-saga-events";

    /**
     * Publie un événement Saga sur Kafka
     * Si Kafka n'est pas disponible, l'événement est loggé mais n'interrompt pas le flux
     */
    public void publishSagaEvent(SagaEvent event) {
        try {
            kafkaTemplate.send(SAGA_EVENTS_TOPIC, event.getTransactionId().toString(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Échec de la publication de l'événement Saga (non bloquant): {} pour la transaction {}. Erreur: {}", 
                                    event.getEventType(), event.getTransactionId(), ex.getMessage());
                        } else {
                            log.info("Événement Saga publié: {} pour la transaction {}", 
                                    event.getEventType(), event.getTransactionId());
                        }
                    });
        } catch (Exception e) {
            // Ne pas bloquer le flux métier si Kafka est indisponible
            log.warn("Erreur lors de la publication de l'événement Saga (non bloquant): {} pour la transaction {}. Erreur: {}", 
                    event.getEventType(), event.getTransactionId(), e.getMessage());
            // Ne pas throw l'exception pour ne pas interrompre le traitement du virement
        }
    }
}

