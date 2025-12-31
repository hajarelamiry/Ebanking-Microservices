package com.example.payment_service.service.impl;

import com.example.payment_service.dto.PaymentRequestDTO;
import com.example.payment_service.dto.PaymentResponseDTO;
import com.example.payment_service.enums.TransactionStatus;
import com.example.payment_service.enums.TransactionType;
import com.example.payment_service.model.Payment;
import com.example.payment_service.repository.PaymentRepository;
import com.example.payment_service.service.FraudDetectionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour PaymentServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests du service de paiement")
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private PaymentRequestDTO validRequest;
    private Payment savedPayment;

    @BeforeEach
    void setUp() {
        validRequest = PaymentRequestDTO.builder()
                .sourceAccountId("ACC123456")
                .destinationIban("FR1420041010050500013M02606")
                .amount(500.0)
                .type(TransactionType.STANDARD)
                .build();

        savedPayment = Payment.builder()
                .id(1L)
                .sourceAccountId("ACC123456")
                .destinationIban("FR1420041010050500013M02606")
                .amount(500.0)
                .type(TransactionType.STANDARD)
                .status(TransactionStatus.PENDING)
                .createdAt(java.time.LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Devrait créer une transaction avec succès pour un montant normal")
    void shouldCreatePaymentSuccessfully() {
        // Given
        when(fraudDetectionService.checkFraudRules(any(PaymentRequestDTO.class)))
                .thenReturn(new FraudDetectionService.FraudCheckResult(TransactionStatus.PENDING, "Transaction créée avec succès"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(1L);
            }
            if (payment.getCreatedAt() == null) {
                payment.setCreatedAt(java.time.LocalDateTime.now());
            }
            return payment;
        });

        // When
        PaymentResponseDTO response = paymentService.initiatePayment(validRequest);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("ACC123456", response.getSourceAccountId());
        assertEquals(500.0, response.getAmount());
        assertEquals(TransactionStatus.VALIDATED, response.getStatus());
        assertNotNull(response.getCreatedAt());

        // Vérifier que save a été appelé deux fois (création + mise à jour après legacy)
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(fraudDetectionService).checkFraudRules(any(PaymentRequestDTO.class));
    }

    @Test
    @DisplayName("Devrait rejeter une transaction si le montant dépasse 10 000€")
    void shouldRejectPaymentWhenAmountExceedsThreshold() {
        // Given
        validRequest.setAmount(15000.0);
        savedPayment.setAmount(15000.0);
        savedPayment.setStatus(TransactionStatus.REJECTED);
        
        when(fraudDetectionService.checkFraudRules(any(PaymentRequestDTO.class)))
                .thenReturn(new FraudDetectionService.FraudCheckResult(TransactionStatus.REJECTED, 
                        "Transaction rejetée: montant supérieur au seuil autorisé (15000.00€ > 10000.00€)"));
        when(paymentRepository.save(any(Payment.class))).thenReturn(savedPayment);

        // When
        PaymentResponseDTO response = paymentService.initiatePayment(validRequest);

        // Then
        assertNotNull(response);
        assertEquals(TransactionStatus.REJECTED, response.getStatus());
        assertTrue(response.getMessage().contains("rejetée"));
        assertTrue(response.getMessage().contains("15000"));
        
        // Vérifier que le statut REJECTED a été défini
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        assertEquals(TransactionStatus.REJECTED, paymentCaptor.getValue().getStatus());
        
        // Vérifier que le legacy n'a pas été appelé pour une transaction rejetée
        verify(paymentRepository, times(1)).save(any(Payment.class));
        verify(fraudDetectionService).checkFraudRules(any(PaymentRequestDTO.class));
    }

    @Test
    @DisplayName("Devrait accepter une transaction avec un montant exactement à 10 000€")
    void shouldAcceptPaymentAtExactThreshold() {
        // Given
        validRequest.setAmount(10000.0);
        savedPayment.setAmount(10000.0);
        
        when(fraudDetectionService.checkFraudRules(any(PaymentRequestDTO.class)))
                .thenReturn(new FraudDetectionService.FraudCheckResult(TransactionStatus.PENDING, "Transaction créée avec succès"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            payment.setCreatedAt(java.time.LocalDateTime.now());
            return payment;
        });

        // When
        PaymentResponseDTO response = paymentService.initiatePayment(validRequest);

        // Then
        assertNotNull(response);
        assertEquals(TransactionStatus.VALIDATED, response.getStatus());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(fraudDetectionService).checkFraudRules(any(PaymentRequestDTO.class));
    }

    @Test
    @DisplayName("Devrait gérer un virement INSTANT correctement")
    void shouldHandleInstantPayment() {
        // Given
        validRequest.setType(TransactionType.INSTANT);
        savedPayment.setType(TransactionType.INSTANT);
        
        when(fraudDetectionService.checkFraudRules(any(PaymentRequestDTO.class)))
                .thenReturn(new FraudDetectionService.FraudCheckResult(TransactionStatus.PENDING, "Transaction créée avec succès"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(1L);
            payment.setCreatedAt(java.time.LocalDateTime.now());
            return payment;
        });

        // When
        PaymentResponseDTO response = paymentService.initiatePayment(validRequest);

        // Then
        assertNotNull(response);
        assertEquals(TransactionType.INSTANT, validRequest.getType());
        assertEquals(TransactionStatus.VALIDATED, response.getStatus());
        verify(paymentRepository, times(2)).save(any(Payment.class));
        verify(fraudDetectionService).checkFraudRules(any(PaymentRequestDTO.class));
    }

    @Test
    @DisplayName("Devrait enregistrer toutes les informations de la transaction")
    void shouldSaveAllPaymentInformation() {
        // Given
        when(fraudDetectionService.checkFraudRules(any(PaymentRequestDTO.class)))
                .thenReturn(new FraudDetectionService.FraudCheckResult(TransactionStatus.PENDING, "Transaction créée avec succès"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            if (payment.getId() == null) {
                payment.setId(1L);
            }
            if (payment.getCreatedAt() == null) {
                payment.setCreatedAt(java.time.LocalDateTime.now());
            }
            return payment;
        });

        // When
        paymentService.initiatePayment(validRequest);

        // Then
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository, atLeastOnce()).save(paymentCaptor.capture());
        
        Payment capturedPayment = paymentCaptor.getValue();
        assertEquals("ACC123456", capturedPayment.getSourceAccountId());
        assertEquals("FR1420041010050500013M02606", capturedPayment.getDestinationIban());
        assertEquals(500.0, capturedPayment.getAmount());
        assertEquals(TransactionType.STANDARD, capturedPayment.getType());
        assertNotNull(capturedPayment.getCreatedAt());
    }
}

