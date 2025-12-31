package com.example.audit_service.controller;

import com.example.audit_service.dto.AuditEventDTO;
import com.example.audit_service.model.AuditLog;
import com.example.audit_service.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditController auditController;

    private AuditEventDTO auditEventDTO;
    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditEventDTO = new AuditEventDTO();
        auditEventDTO.setUserId("user123");
        auditEventDTO.setActionType("CRYPTO_BUY");
        auditEventDTO.setServiceName("crypto-service");
        auditEventDTO.setDescription("Achat de Bitcoin");
        auditEventDTO.setStatus("SUCCESS");

        auditLog = new AuditLog();
        auditLog.setId(1L);
        auditLog.setUserId("user123");
        auditLog.setActionType("CRYPTO_BUY");
        auditLog.setServiceName("crypto-service");
        auditLog.setDescription("Achat de Bitcoin");
        auditLog.setStatus("SUCCESS");
        auditLog.setTimestamp(LocalDateTime.now());
    }

    @Test
    void testLogEvent_ShouldReturnCreated() {
        // Given
        when(auditService.logEvent(any(AuditEventDTO.class), any())).thenReturn(auditLog);

        // When
        ResponseEntity<Map<String, Object>> response = auditController.logEvent(auditEventDTO, null);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Audit event logged successfully", response.getBody().get("message"));
        assertEquals(1L, response.getBody().get("auditLogId"));
    }

    @Test
    void testReceiveExternalEvent_ShouldReturnCreated() {
        // Given
        when(auditService.receiveEventFromService(any(AuditEventDTO.class))).thenReturn(auditLog);

        // When
        ResponseEntity<Map<String, Object>> response = auditController.receiveExternalEvent(auditEventDTO);

        // Then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("External audit event received and logged", response.getBody().get("message"));
        assertEquals(1L, response.getBody().get("auditLogId"));
    }

    @Test
    void testGetUserHistory_ShouldReturnOk() {
        // Given
        Page<AuditLog> pagedAuditLogs = new PageImpl<>(Arrays.asList(auditLog));
        when(auditService.getUserHistory(eq("user123"), any())).thenReturn(pagedAuditLogs);

        // When
        ResponseEntity<Map<String, Object>> response = auditController.getUserHistory(
                "user123", 0, 20, null, null, null, null
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("user123", response.getBody().get("userId"));
        assertEquals(1L, response.getBody().get("totalElements"));
    }

    @Test
    void testGetAllHistory_ShouldReturnOk() {
        // Given
        Page<AuditLog> pagedAuditLogs = new PageImpl<>(Arrays.asList(auditLog));
        when(auditService.getAllHistory(any())).thenReturn(pagedAuditLogs);

        // When
        ResponseEntity<Map<String, Object>> response = auditController.getAllHistory(
                0, 20, null, null, null, null, null, null
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1L, response.getBody().get("totalElements"));
    }

    @Test
    void testGetErrorsAndFailures_ShouldReturnOk() {
        // Given
        AuditLog errorLog = new AuditLog();
        errorLog.setStatus("ERROR");
        Page<AuditLog> pagedAuditLogs = new PageImpl<>(Arrays.asList(errorLog));
        when(auditService.getErrorsAndFailures(any())).thenReturn(pagedAuditLogs);

        // When
        ResponseEntity<Map<String, Object>> response = auditController.getErrorsAndFailures(0, 20);

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void testGetUserStats_ShouldReturnOk() {
        // Given
        when(auditService.getUserActionCount("user123")).thenReturn(5L);

        // When
        ResponseEntity<Map<String, Object>> response = auditController.getUserStats("user123");

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("user123", response.getBody().get("userId"));
        assertEquals(5L, response.getBody().get("totalActions"));
    }

    @Test
    void testGetErrorStats_ShouldReturnOk() {
        // Given
        when(auditService.getErrorCount()).thenReturn(3L);

        // When
        ResponseEntity<Map<String, Object>> response = auditController.getErrorStats();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3L, response.getBody().get("totalErrors"));
    }

    @Test
    void testHealth_ShouldReturnOk() {
        // When
        ResponseEntity<Map<String, String>> response = auditController.health();

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("audit-service", response.getBody().get("service"));
    }
}
