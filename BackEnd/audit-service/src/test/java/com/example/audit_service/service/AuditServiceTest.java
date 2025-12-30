package com.example.audit_service.service;

import com.example.audit_service.dto.AuditEventDTO;
import com.example.audit_service.model.AuditLog;
import com.example.audit_service.repository.AuditRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private AuditService auditService;

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
    void testLogEvent_ShouldSaveAuditLog() {
        // Given
        when(auditRepository.save(any(AuditLog.class))).thenReturn(auditLog);

        // When
        AuditLog result = auditService.logEvent(auditEventDTO);

        // Then
        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals("CRYPTO_BUY", result.getActionType());
        verify(auditRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testLogEvent_WithHttpServletRequest_ShouldExtractIpAndUserAgent() {
        // Given
        when(httpServletRequest.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");
        when(httpServletRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(auditRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(1L);
            return log;
        });

        // When
        AuditLog result = auditService.logEvent(auditEventDTO, httpServletRequest);

        // Then
        assertNotNull(result);
        verify(auditRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testReceiveEventFromService_ShouldSaveExternalEvent() {
        // Given
        auditEventDTO.setServiceName("payment-service");
        AuditLog savedLog = new AuditLog();
        savedLog.setId(1L);
        savedLog.setUserId("user123");
        savedLog.setActionType("CRYPTO_BUY");
        savedLog.setServiceName("payment-service");
        savedLog.setDescription("Achat de Bitcoin");
        savedLog.setStatus("SUCCESS");
        savedLog.setTimestamp(LocalDateTime.now());
        when(auditRepository.save(any(AuditLog.class))).thenAnswer(invocation -> {
            AuditLog log = invocation.getArgument(0);
            log.setId(1L);
            return log;
        });

        // When
        AuditLog result = auditService.receiveEventFromService(auditEventDTO);

        // Then
        assertNotNull(result);
        assertEquals("payment-service", result.getServiceName());
        verify(auditRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    void testReceiveEventFromService_WithoutServiceName_ShouldThrowException() {
        // Given
        auditEventDTO.setServiceName(null);

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            auditService.receiveEventFromService(auditEventDTO);
        });
    }

    @Test
    void testGetUserHistory_ShouldReturnUserAuditLogs() {
        // Given
        List<AuditLog> auditLogs = Arrays.asList(auditLog);
        when(auditRepository.findByUserIdOrderByTimestampDesc("user123")).thenReturn(auditLogs);

        // When
        List<AuditLog> result = auditService.getUserHistory("user123");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("user123", result.get(0).getUserId());
        verify(auditRepository, times(1)).findByUserIdOrderByTimestampDesc("user123");
    }

    @Test
    void testGetUserHistory_WithPagination_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> pagedAuditLogs = new PageImpl<>(Arrays.asList(auditLog));
        when(auditRepository.findByUserIdOrderByTimestampDesc("user123", pageable))
                .thenReturn(pagedAuditLogs);

        // When
        Page<AuditLog> result = auditService.getUserHistory("user123", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(auditRepository, times(1)).findByUserIdOrderByTimestampDesc("user123", pageable);
    }

    @Test
    void testGetAllHistory_ShouldReturnAllAuditLogs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> pagedAuditLogs = new PageImpl<>(Arrays.asList(auditLog));
        when(auditRepository.findAllByOrderByTimestampDesc(pageable)).thenReturn(pagedAuditLogs);

        // When
        Page<AuditLog> result = auditService.getAllHistory(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(auditRepository, times(1)).findAllByOrderByTimestampDesc(pageable);
    }

    @Test
    void testGetErrorsAndFailures_ShouldReturnErrorLogs() {
        // Given
        AuditLog errorLog = new AuditLog();
        errorLog.setStatus("ERROR");
        errorLog.setErrorMessage("Transaction failed");
        List<AuditLog> errorLogs = Arrays.asList(errorLog);
        when(auditRepository.findByStatusInOrderByTimestampDesc(Arrays.asList("FAILURE", "ERROR")))
                .thenReturn(errorLogs);

        // When
        List<AuditLog> result = auditService.getErrorsAndFailures();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ERROR", result.get(0).getStatus());
        verify(auditRepository, times(1))
                .findByStatusInOrderByTimestampDesc(Arrays.asList("FAILURE", "ERROR"));
    }

    @Test
    void testGetUserActionCount_ShouldReturnCount() {
        // Given
        when(auditRepository.countByUserId("user123")).thenReturn(5L);

        // When
        Long result = auditService.getUserActionCount("user123");

        // Then
        assertEquals(5L, result);
        verify(auditRepository, times(1)).countByUserId("user123");
    }

    @Test
    void testGetErrorCount_ShouldReturnErrorCount() {
        // Given
        when(auditRepository.countByStatusIn(Arrays.asList("FAILURE", "ERROR"))).thenReturn(3L);

        // When
        Long result = auditService.getErrorCount();

        // Then
        assertEquals(3L, result);
        verify(auditRepository, times(1)).countByStatusIn(Arrays.asList("FAILURE", "ERROR"));
    }
}

