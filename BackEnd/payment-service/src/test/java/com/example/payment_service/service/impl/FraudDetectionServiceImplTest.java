package com.example.payment_service.service.impl;

import com.example.payment_service.dto.PaymentRequestDTO;
import com.example.payment_service.enums.TransactionStatus;
import com.example.payment_service.enums.TransactionType;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.FraudDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour FraudDetectionServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du service de détection de fraude")
class FraudDetectionServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private FraudDetectionServiceImpl fraudDetectionService;

    private PaymentRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        validRequest = PaymentRequestDTO.builder()
                .sourceAccountId("ACC123456")
                .destinationIban("FR1420041010050500013M02606")
                .amount(500.0)
                .type(TransactionType.STANDARD)
                .build();
    }

    @Test
    @DisplayName("Devrait rejeter si montant > 10 000€")
    void shouldRejectWhenAmountExceedsThreshold() {
        // Given
        validRequest.setAmount(15000.0);

        // When
        FraudDetectionService.FraudCheckResult result = fraudDetectionService.checkFraudRules(validRequest);

        // Then
        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertTrue(result.getMessage().contains("montant supérieur au seuil autorisé"));
    }

    @Test
    @DisplayName("Devrait rejeter si plus de 3 virements en 10 minutes")
    void shouldRejectWhenVelocityExceeded() {
        // Given
        when(paymentRepository.countBySourceAccountIdAndCreatedAtAfter(anyString(), any())).thenReturn(3L);

        // When
        FraudDetectionService.FraudCheckResult result = fraudDetectionService.checkFraudRules(validRequest);

        // Then
        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertTrue(result.getMessage().contains("trop de virements récents"));
    }

    @Test
    @DisplayName("Devrait rejeter si cumul journalier > 15 000€")
    void shouldRejectWhenDailyCumulativeExceeded() {
        // Given
        validRequest.setAmount(8000.0);
        when(paymentRepository.countBySourceAccountIdAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
        when(paymentRepository.sumAmountBySourceAccountIdAndCreatedAtAfter(anyString(), any())).thenReturn(10000.0); // Cumul existant

        // When
        FraudDetectionService.FraudCheckResult result = fraudDetectionService.checkFraudRules(validRequest);

        // Then
        assertEquals(TransactionStatus.REJECTED, result.getStatus());
        assertTrue(result.getMessage().contains("cumul journalier dépassé"));
        assertTrue(result.getMessage().contains("18000")); // 10000 + 8000
    }

    @Test
    @DisplayName("Devrait mettre en PENDING_MANUAL_REVIEW pour nouveau bénéficiaire avec montant > 2000€")
    void shouldSetPendingManualReviewForNewBeneficiaryWithHighAmount() {
        // Given
        validRequest.setAmount(2500.0);
        when(paymentRepository.countBySourceAccountIdAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
        when(paymentRepository.sumAmountBySourceAccountIdAndCreatedAtAfter(anyString(), any())).thenReturn(0.0);
        when(paymentRepository.existsBySourceAccountIdAndDestinationIban(anyString(), anyString())).thenReturn(false);

        // When
        FraudDetectionService.FraudCheckResult result = fraudDetectionService.checkFraudRules(validRequest);

        // Then
        assertEquals(TransactionStatus.PENDING_MANUAL_REVIEW, result.getStatus());
        assertTrue(result.getMessage().contains("validation manuelle"));
        assertTrue(result.getMessage().contains("nouveau bénéficiaire"));
    }

    @Test
    @DisplayName("Devrait accepter un nouveau bénéficiaire si montant <= 2000€")
    void shouldAcceptNewBeneficiaryWithLowAmount() {
        // Given
        validRequest.setAmount(1500.0);
        when(paymentRepository.countBySourceAccountIdAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
        when(paymentRepository.sumAmountBySourceAccountIdAndCreatedAtAfter(anyString(), any())).thenReturn(0.0);
        when(paymentRepository.existsBySourceAccountIdAndDestinationIban(anyString(), anyString())).thenReturn(false);

        // When
        FraudDetectionService.FraudCheckResult result = fraudDetectionService.checkFraudRules(validRequest);

        // Then
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        assertEquals("Transaction créée avec succès", result.getMessage());
    }

    @Test
    @DisplayName("Devrait accepter une transaction valide")
    void shouldAcceptValidTransaction() {
        // Given
        when(paymentRepository.countBySourceAccountIdAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
        when(paymentRepository.sumAmountBySourceAccountIdAndCreatedAtAfter(anyString(), any())).thenReturn(0.0);
        when(paymentRepository.existsBySourceAccountIdAndDestinationIban(anyString(), anyString())).thenReturn(true);

        // When
        FraudDetectionService.FraudCheckResult result = fraudDetectionService.checkFraudRules(validRequest);

        // Then
        assertEquals(TransactionStatus.PENDING, result.getStatus());
        assertEquals("Transaction créée avec succès", result.getMessage());
    }

    @Test
    @DisplayName("Devrait accepter si cumul journalier exactement à 15 000€")
    void shouldAcceptWhenDailyCumulativeAtExactThreshold() {
        // Given
        validRequest.setAmount(5000.0);
        when(paymentRepository.countBySourceAccountIdAndCreatedAtAfter(anyString(), any())).thenReturn(0L);
        when(paymentRepository.sumAmountBySourceAccountIdAndCreatedAtAfter(anyString(), any())).thenReturn(10000.0); // Cumul = 10000
        when(paymentRepository.existsBySourceAccountIdAndDestinationIban(anyString(), anyString())).thenReturn(true);

        // When
        FraudDetectionService.FraudCheckResult result = fraudDetectionService.checkFraudRules(validRequest);

        // Then
        assertEquals(TransactionStatus.PENDING, result.getStatus()); // 10000 + 5000 = 15000 (égal, pas supérieur)
    }
}

