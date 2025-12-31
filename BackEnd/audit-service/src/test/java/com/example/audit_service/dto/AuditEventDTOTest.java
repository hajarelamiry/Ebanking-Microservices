package com.example.audit_service.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuditEventDTOTest {

    private Validator validator;
    private AuditEventDTO auditEventDTO;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        auditEventDTO = new AuditEventDTO();
    }

    @Test
    void testDefaultConstructor() {
        // When
        AuditEventDTO dto = new AuditEventDTO();

        // Then
        assertNotNull(dto);
    }

    @Test
    void testParameterizedConstructor() {
        // When
        AuditEventDTO dto = new AuditEventDTO("user123", "CRYPTO_BUY", 
                                             "crypto-service", "Achat", "SUCCESS");

        // Then
        assertEquals("user123", dto.getUserId());
        assertEquals("CRYPTO_BUY", dto.getActionType());
        assertEquals("crypto-service", dto.getServiceName());
        assertEquals("Achat", dto.getDescription());
        assertEquals("SUCCESS", dto.getStatus());
    }

    @Test
    void testGettersAndSetters() {
        // When
        auditEventDTO.setUserId("user123");
        auditEventDTO.setActionType("TRANSFER");
        auditEventDTO.setServiceName("payment-service");
        auditEventDTO.setDescription("Virement");
        auditEventDTO.setDetails("{\"amount\": 1000}");
        auditEventDTO.setStatus("SUCCESS");
        auditEventDTO.setErrorMessage(null);
        auditEventDTO.setIpAddress("192.168.1.1");
        auditEventDTO.setUserAgent("Mozilla/5.0");

        // Then
        assertEquals("user123", auditEventDTO.getUserId());
        assertEquals("TRANSFER", auditEventDTO.getActionType());
        assertEquals("payment-service", auditEventDTO.getServiceName());
        assertEquals("Virement", auditEventDTO.getDescription());
        assertEquals("{\"amount\": 1000}", auditEventDTO.getDetails());
        assertEquals("SUCCESS", auditEventDTO.getStatus());
        assertNull(auditEventDTO.getErrorMessage());
        assertEquals("192.168.1.1", auditEventDTO.getIpAddress());
        assertEquals("Mozilla/5.0", auditEventDTO.getUserAgent());
    }

    @Test
    void testValidation_WithMissingRequiredFields_ShouldFail() {
        // Given - DTO sans champs requis
        AuditEventDTO invalidDTO = new AuditEventDTO();

        // When
        Set<ConstraintViolation<AuditEventDTO>> violations = validator.validate(invalidDTO);

        // Then
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("userId")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("actionType")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("serviceName")));
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("status")));
    }

    @Test
    void testValidation_WithAllRequiredFields_ShouldPass() {
        // Given
        auditEventDTO.setUserId("user123");
        auditEventDTO.setActionType("CRYPTO_BUY");
        auditEventDTO.setServiceName("crypto-service");
        auditEventDTO.setStatus("SUCCESS");

        // When
        Set<ConstraintViolation<AuditEventDTO>> violations = validator.validate(auditEventDTO);

        // Then
        assertTrue(violations.isEmpty());
    }
}

