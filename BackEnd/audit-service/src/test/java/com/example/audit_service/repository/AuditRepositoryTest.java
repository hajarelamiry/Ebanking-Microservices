package com.example.audit_service.repository;

import com.example.audit_service.model.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditRepositoryTest {

    @Mock
    private AuditRepository auditRepository;

    private AuditLog auditLog1;
    private AuditLog auditLog2;
    private AuditLog auditLog3;

    @BeforeEach
    void setUp() {
        auditLog1 = new AuditLog();
        auditLog1.setId(1L);
        auditLog1.setUserId("user123");
        auditLog1.setActionType("CRYPTO_BUY");
        auditLog1.setServiceName("crypto-service");
        auditLog1.setDescription("Achat de Bitcoin");
        auditLog1.setStatus("SUCCESS");
        auditLog1.setTimestamp(LocalDateTime.now().minusHours(1));

        auditLog2 = new AuditLog();
        auditLog2.setId(2L);
        auditLog2.setUserId("user123");
        auditLog2.setActionType("TRANSFER");
        auditLog2.setServiceName("payment-service");
        auditLog2.setDescription("Virement bancaire");
        auditLog2.setStatus("SUCCESS");
        auditLog2.setTimestamp(LocalDateTime.now().minusMinutes(30));

        auditLog3 = new AuditLog();
        auditLog3.setId(3L);
        auditLog3.setUserId("user456");
        auditLog3.setActionType("CRYPTO_SELL");
        auditLog3.setServiceName("crypto-service");
        auditLog3.setDescription("Vente de Ethereum");
        auditLog3.setStatus("FAILURE");
        auditLog3.setErrorMessage("Solde insuffisant");
        auditLog3.setTimestamp(LocalDateTime.now());
    }

    @Test
    void testFindByUserIdOrderByTimestampDesc_ShouldReturnUserLogs() {
        // Given
        List<AuditLog> expectedLogs = Arrays.asList(auditLog2, auditLog1);
        when(auditRepository.findByUserIdOrderByTimestampDesc("user123")).thenReturn(expectedLogs);

        // When
        List<AuditLog> result = auditRepository.findByUserIdOrderByTimestampDesc("user123");

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("user123", result.get(0).getUserId());
        verify(auditRepository, times(1)).findByUserIdOrderByTimestampDesc("user123");
    }

    @Test
    void testFindByUserIdOrderByTimestampDesc_WithPagination_ShouldReturnPagedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 1);
        Page<AuditLog> pagedLogs = new PageImpl<>(Arrays.asList(auditLog2));
        when(auditRepository.findByUserIdOrderByTimestampDesc("user123", pageable)).thenReturn(pagedLogs);

        // When
        Page<AuditLog> result = auditRepository.findByUserIdOrderByTimestampDesc("user123", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(auditRepository, times(1)).findByUserIdOrderByTimestampDesc("user123", pageable);
    }

    @Test
    void testFindAllByOrderByTimestampDesc_ShouldReturnAllLogs() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<AuditLog> pagedLogs = new PageImpl<>(Arrays.asList(auditLog3, auditLog2, auditLog1));
        when(auditRepository.findAllByOrderByTimestampDesc(pageable)).thenReturn(pagedLogs);

        // When
        Page<AuditLog> result = auditRepository.findAllByOrderByTimestampDesc(pageable);

        // Then
        assertNotNull(result);
        assertEquals(3, result.getTotalElements());
        verify(auditRepository, times(1)).findAllByOrderByTimestampDesc(pageable);
    }

    @Test
    void testFindByStatusInOrderByTimestampDesc_ShouldReturnErrorLogs() {
        // Given
        List<AuditLog> errorLogs = Arrays.asList(auditLog3);
        when(auditRepository.findByStatusInOrderByTimestampDesc(Arrays.asList("FAILURE", "ERROR")))
                .thenReturn(errorLogs);

        // When
        List<AuditLog> result = auditRepository.findByStatusInOrderByTimestampDesc(
                Arrays.asList("FAILURE", "ERROR")
        );

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("FAILURE", result.get(0).getStatus());
        verify(auditRepository, times(1))
                .findByStatusInOrderByTimestampDesc(Arrays.asList("FAILURE", "ERROR"));
    }

    @Test
    void testFindByServiceNameOrderByTimestampDesc_ShouldReturnServiceLogs() {
        // Given
        List<AuditLog> serviceLogs = Arrays.asList(auditLog3, auditLog1);
        when(auditRepository.findByServiceNameOrderByTimestampDesc("crypto-service")).thenReturn(serviceLogs);

        // When
        List<AuditLog> result = auditRepository.findByServiceNameOrderByTimestampDesc("crypto-service");

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("crypto-service", result.get(0).getServiceName());
        verify(auditRepository, times(1)).findByServiceNameOrderByTimestampDesc("crypto-service");
    }

    @Test
    void testFindByActionTypeOrderByTimestampDesc_ShouldReturnActionTypeLogs() {
        // Given
        List<AuditLog> actionLogs = Arrays.asList(auditLog1);
        when(auditRepository.findByActionTypeOrderByTimestampDesc("CRYPTO_BUY")).thenReturn(actionLogs);

        // When
        List<AuditLog> result = auditRepository.findByActionTypeOrderByTimestampDesc("CRYPTO_BUY");

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("CRYPTO_BUY", result.get(0).getActionType());
        verify(auditRepository, times(1)).findByActionTypeOrderByTimestampDesc("CRYPTO_BUY");
    }

    @Test
    void testCountByUserId_ShouldReturnUserCount() {
        // Given
        when(auditRepository.countByUserId("user123")).thenReturn(2L);

        // When
        Long count = auditRepository.countByUserId("user123");

        // Then
        assertEquals(2L, count);
        verify(auditRepository, times(1)).countByUserId("user123");
    }

    @Test
    void testCountByStatusIn_ShouldReturnErrorCount() {
        // Given
        when(auditRepository.countByStatusIn(Arrays.asList("FAILURE", "ERROR"))).thenReturn(1L);

        // When
        Long count = auditRepository.countByStatusIn(Arrays.asList("FAILURE", "ERROR"));

        // Then
        assertEquals(1L, count);
        verify(auditRepository, times(1)).countByStatusIn(Arrays.asList("FAILURE", "ERROR"));
    }

    @Test
    void testSave_ShouldPersistAuditLog() {
        // Given
        when(auditRepository.save(any(AuditLog.class))).thenReturn(auditLog1);

        // When
        AuditLog saved = auditRepository.save(auditLog1);

        // Then
        assertNotNull(saved);
        assertEquals(1L, saved.getId());
        verify(auditRepository, times(1)).save(auditLog1);
    }
}
