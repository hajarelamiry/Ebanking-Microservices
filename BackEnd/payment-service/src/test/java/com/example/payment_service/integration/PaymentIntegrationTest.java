package com.example.payment_service.integration;

import com.example.payment_service.dto.PaymentRequestDTO;
import com.example.payment_service.dto.PaymentResponseDTO;
import com.example.payment_service.enums.TransactionStatus;
import com.example.payment_service.enums.TransactionType;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.AuditService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Tests d'intégration pour Payment Service
 * Teste le flux complet avec base de données réelle
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentIntegrationTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private com.example.payment_service.service.PaymentService paymentService;

    @MockBean
    private AuditService auditService; // Mock pour éviter l'appel réel à audit-service

    @Test
    void shouldCreateAndSavePayment() {
        // Given
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .sourceAccountId("ACC123456")
                .destinationIban("FR1420041010050500013M02606")
                .amount(500.0)
                .type(TransactionType.STANDARD)
                .build();

        // When
        PaymentResponseDTO response = paymentService.initiatePayment(request);

        // Then
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("ACC123456", response.getSourceAccountId());
        assertEquals(500.0, response.getAmount());
        assertNotNull(response.getCreatedAt());

        // Vérifier que le payment est bien enregistré en base
        assertTrue(paymentRepository.findById(response.getId()).isPresent());

        // Vérifier que l'audit a été appelé
        verify(auditService).sendAuditEvent(any());
    }

    @Test
    void shouldRejectPaymentWithHighAmount() {
        // Given
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .sourceAccountId("ACC123456")
                .destinationIban("FR1420041010050500013M02606")
                .amount(15000.0)
                .type(TransactionType.STANDARD)
                .build();

        // When
        PaymentResponseDTO response = paymentService.initiatePayment(request);

        // Then
        assertNotNull(response);
        assertEquals(TransactionStatus.REJECTED, response.getStatus());
        assertTrue(response.getMessage().contains("rejetée"));

        // Vérifier que l'audit a été appelé même pour un rejet
        verify(auditService).sendAuditEvent(any());
    }
}
