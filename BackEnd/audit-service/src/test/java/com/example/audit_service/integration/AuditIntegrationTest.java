package com.example.audit_service.integration;

import com.example.audit_service.dto.AuditEventDTO;
import com.example.audit_service.model.AuditLog;
import com.example.audit_service.repository.AuditRepository;
import com.example.audit_service.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour Audit Service
 * Teste le flux complet avec base de données réelle
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private AuditRepository auditRepository;

    @Test
    void shouldSaveAuditEvent() {
        // Given
        AuditEventDTO eventDTO = new AuditEventDTO();
        eventDTO.setUserId("user123");
        eventDTO.setActionType("PAYMENT_CREATED");
        eventDTO.setServiceName("payment-service");
        eventDTO.setDescription("Payment transaction created");
        eventDTO.setStatus("SUCCESS");

        // When
        AuditLog savedLog = auditService.logEvent(eventDTO);

        // Then
        assertNotNull(savedLog);
        assertNotNull(savedLog.getId());
        assertEquals("user123", savedLog.getUserId());
        assertEquals("PAYMENT_CREATED", savedLog.getActionType());
        assertEquals("payment-service", savedLog.getServiceName());
        assertEquals("SUCCESS", savedLog.getStatus());

        // Vérifier que c'est bien enregistré en base
        assertTrue(auditRepository.findById(savedLog.getId()).isPresent());
    }

    @Test
    void shouldReceiveExternalEvent() {
        // Given
        AuditEventDTO eventDTO = new AuditEventDTO();
        eventDTO.setUserId("user456");
        eventDTO.setActionType("CRYPTO_BUY");
        eventDTO.setServiceName("crypto-service");
        eventDTO.setDescription("Crypto buy transaction");
        eventDTO.setStatus("SUCCESS");

        // When
        AuditLog savedLog = auditService.receiveEventFromService(eventDTO);

        // Then
        assertNotNull(savedLog);
        assertEquals("crypto-service", savedLog.getServiceName());
        assertTrue(auditRepository.findById(savedLog.getId()).isPresent());
    }

    @Test
    void shouldGetUserHistory() {
        // Given
        String userId = "user789";
        
        AuditEventDTO event1 = new AuditEventDTO();
        event1.setUserId(userId);
        event1.setActionType("LOGIN");
        event1.setServiceName("auth-service");
        event1.setStatus("SUCCESS");
        auditService.logEvent(event1);

        AuditEventDTO event2 = new AuditEventDTO();
        event2.setUserId(userId);
        event2.setActionType("PAYMENT_CREATED");
        event2.setServiceName("payment-service");
        event2.setStatus("SUCCESS");
        auditService.logEvent(event2);

        // When
        Page<AuditLog> history = auditService.getUserHistory(userId, PageRequest.of(0, 10));

        // Then
        assertNotNull(history);
        assertTrue(history.getTotalElements() >= 2);
        assertTrue(history.getContent().stream()
                .allMatch(log -> userId.equals(log.getUserId())));
    }

    @Test
    void shouldGetHistoryByServiceName() {
        // Given
        AuditEventDTO event = new AuditEventDTO();
        event.setUserId("user999");
        event.setActionType("PAYMENT_CREATED");
        event.setServiceName("payment-service");
        event.setStatus("SUCCESS");
        auditService.logEvent(event);

        // When
        Page<AuditLog> history = auditService.getHistoryByServiceName("payment-service", PageRequest.of(0, 10));

        // Then
        assertNotNull(history);
        assertTrue(history.getTotalElements() >= 1);
        assertTrue(history.getContent().stream()
                .anyMatch(log -> "payment-service".equals(log.getServiceName())));
    }

    @Test
    void shouldGetErrors() {
        // Given
        AuditEventDTO errorEvent = new AuditEventDTO();
        errorEvent.setUserId("user111");
        errorEvent.setActionType("PAYMENT_REJECTED");
        errorEvent.setServiceName("payment-service");
        errorEvent.setStatus("FAILURE");
        errorEvent.setErrorMessage("Transaction rejected");
        auditService.logEvent(errorEvent);

        // When
        Page<AuditLog> errors = auditService.getErrorsAndFailures(PageRequest.of(0, 10));

        // Then
        assertNotNull(errors);
        assertTrue(errors.getTotalElements() >= 1);
        assertTrue(errors.getContent().stream()
                .anyMatch(log -> "FAILURE".equals(log.getStatus()) || "ERROR".equals(log.getStatus())));
    }
}
