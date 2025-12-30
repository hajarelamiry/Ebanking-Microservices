package com.example.payment_service.service;

import com.example.payment_service.model.OutboxEvent;
import com.example.payment_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Service qui lit les événements de la table outbox et les publie dans Kafka
 * Ce service s'exécute périodiquement pour garantir la publication des événements
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    private static final String AUDIT_TOPIC = "audit-events";
    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 100;

    /**
     * Lit les événements en attente de la table outbox et les publie dans Kafka
     * Cette méthode s'exécute toutes les 5 secondes
     */
    @Scheduled(fixedRate = 5000) // Toutes les 5 secondes
    @Transactional
    public void relayEvents() {
        try {
            // Récupère les événements en attente
            List<OutboxEvent> pendingEvents = outboxEventRepository
                    .findByStatusOrderByCreatedAtAsc(OutboxEvent.OutboxStatus.PENDING);
            
            if (pendingEvents.isEmpty()) {
                return;
            }
            
            log.debug("Found {} pending events to relay", pendingEvents.size());
            
            // Traite les événements par batch
            int processed = 0;
            for (OutboxEvent event : pendingEvents) {
                if (processed >= BATCH_SIZE) {
                    break;
                }
                
                try {
                    // Publie l'événement dans Kafka
                    publishToKafka(event);
                    
                    // Marque l'événement comme publié
                    outboxEventRepository.markAsPublished(event.getId(), LocalDateTime.now());
                    
                    log.debug("Event relayed successfully: id={}, type={}", event.getId(), event.getEventType());
                    processed++;
                    
                } catch (Exception e) {
                    log.error("Failed to relay event: id={}, type={}", event.getId(), event.getEventType(), e);
                    
                    // Marque l'événement comme échoué si le nombre de tentatives dépasse le maximum
                    if (event.getRetryCount() >= MAX_RETRIES) {
                        outboxEventRepository.markAsFailed(event.getId(), 
                                "Max retries exceeded: " + e.getMessage());
                        log.error("Event marked as failed after {} retries: id={}", 
                                event.getRetryCount(), event.getId());
                    } else {
                        // Réinitialise pour réessayer
                        outboxEventRepository.markAsFailed(event.getId(), e.getMessage());
                        outboxEventRepository.resetForRetry(event.getId(), MAX_RETRIES);
                    }
                }
            }
            
            if (processed > 0) {
                log.info("Relayed {} events to Kafka", processed);
            }
            
        } catch (Exception e) {
            log.error("Error in outbox relay process", e);
        }
    }

    /**
     * Publie un événement dans Kafka
     */
    private void publishToKafka(OutboxEvent event) {
        // Utilise le correlation_id comme clé de partition pour garantir l'ordre
        String key = event.getCorrelationId() != null ? event.getCorrelationId() : event.getId().toString();
        
        try {
            // Publie dans le topic Kafka de manière synchrone pour gérer les erreurs
            var result = kafkaTemplate.send(AUDIT_TOPIC, key, event.getPayload()).get();
            log.debug("Event sent to Kafka successfully: id={}, offset={}", 
                    event.getId(), result.getRecordMetadata().offset());
        } catch (InterruptedException | ExecutionException e) {
            log.error("Failed to send event to Kafka: id={}", event.getId(), e);
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to send event to Kafka", e);
        }
    }
}

