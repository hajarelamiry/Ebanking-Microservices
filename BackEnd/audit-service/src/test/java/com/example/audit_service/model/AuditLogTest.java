package com.example.audit_service.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuditLogTest {

    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
    }

    @Test
    void testDefaultConstructor() {
        // When
        AuditLog log = new AuditLog();

        // Then
        assertNotNull(log);
    }

    @Test
    void testParameterizedConstructor() {
        // When
        AuditLog log = new AuditLog("user123", "CRYPTO_BUY", "crypto-service", 
                                    "Achat de Bitcoin", "SUCCESS");

        // Then
        assertEquals("user123", log.getUserId());
        assertEquals("CRYPTO_BUY", log.getActionType());
        assertEquals("crypto-service", log.getServiceName());
        assertEquals("Achat de Bitcoin", log.getDescription());
        assertEquals("SUCCESS", log.getStatus());
        assertNotNull(log.getTimestamp());
    }

    @Test
    void testGettersAndSetters() {
        // Given
        LocalDateTime timestamp = LocalDateTime.now();

        // When
        auditLog.setId(1L);
        auditLog.setUserId("user123");
        auditLog.setActionType("TRANSFER");
        auditLog.setServiceName("payment-service");
        auditLog.setDescription("Virement");
        auditLog.setDetails("{\"amount\": 1000}");
        auditLog.setTimestamp(timestamp);
        auditLog.setStatus("SUCCESS");
        auditLog.setErrorMessage(null);
        auditLog.setIpAddress("192.168.1.1");
        auditLog.setUserAgent("Mozilla/5.0");

        // Then
        assertEquals(1L, auditLog.getId());
        assertEquals("user123", auditLog.getUserId());
        assertEquals("TRANSFER", auditLog.getActionType());
        assertEquals("payment-service", auditLog.getServiceName());
        assertEquals("Virement", auditLog.getDescription());
        assertEquals("{\"amount\": 1000}", auditLog.getDetails());
        assertEquals(timestamp, auditLog.getTimestamp());
        assertEquals("SUCCESS", auditLog.getStatus());
        assertNull(auditLog.getErrorMessage());
        assertEquals("192.168.1.1", auditLog.getIpAddress());
        assertEquals("Mozilla/5.0", auditLog.getUserAgent());
    }

    @Test
    void testPrePersist_ShouldSetTimestamp() {
        // Given
        AuditLog log = new AuditLog("user123", "CRYPTO_BUY", "crypto-service", 
                                    "Achat de Bitcoin", "SUCCESS");

        // Then - Le timestamp devrait être défini automatiquement par @PrePersist
        assertNotNull(log.getTimestamp());
    }
}

