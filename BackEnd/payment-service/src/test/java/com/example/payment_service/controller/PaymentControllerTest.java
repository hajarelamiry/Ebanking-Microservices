package com.example.payment_service.controller;

import com.example.payment_service.dto.PaymentRequestDTO;
import com.example.payment_service.dto.PaymentResponseDTO;
import com.example.payment_service.enums.TransactionStatus;
import com.example.payment_service.enums.TransactionType;
import com.example.payment_service.service.PaymentService;
import com.example.payment_service.service.impl.PaymentServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour PaymentController
 */
@WebMvcTest(PaymentController.class)
@DisplayName("Tests du contrôleur de paiement")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Devrait créer un virement avec succès")
    void shouldCreatePaymentSuccessfully() throws Exception {
        // Given
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .sourceAccountId("ACC123456")
                .destinationIban("FR1420041010050500013M02606")
                .amount(500.0)
                .type(TransactionType.STANDARD)
                .build();

        PaymentResponseDTO response = PaymentResponseDTO.builder()
                .id(1L)
                .sourceAccountId("ACC123456")
                .destinationIban("FR1420041010050500013M02606")
                .amount(500.0)
                .status(TransactionStatus.VALIDATED)
                .message("Transaction créée avec succès")
                .createdAt(LocalDateTime.now())
                .build();

        when(paymentService.initiatePayment(any(PaymentRequestDTO.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.sourceAccountId").value("ACC123456"))
                .andExpect(jsonPath("$.amount").value(500.0))
                .andExpect(jsonPath("$.status").value("VALIDATED"));
    }

    @Test
    @DisplayName("Devrait retourner 422 pour une transaction rejetée")
    void shouldReturn422ForRejectedPayment() throws Exception {
        // Given
        PaymentRequestDTO request = PaymentRequestDTO.builder()
                .sourceAccountId("ACC123456")
                .destinationIban("FR1420041010050500013M02606")
                .amount(15000.0)
                .type(TransactionType.STANDARD)
                .build();

        PaymentResponseDTO response = PaymentResponseDTO.builder()
                .id(1L)
                .status(TransactionStatus.REJECTED)
                .message("Transaction rejetée: montant supérieur au seuil autorisé")
                .build();

        when(paymentService.initiatePayment(any(PaymentRequestDTO.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    @DisplayName("Devrait retourner 400 pour une requête invalide")
    void shouldReturn400ForInvalidRequest() throws Exception {
        // Given - requête avec champs manquants
        PaymentRequestDTO invalidRequest = PaymentRequestDTO.builder()
                .sourceAccountId("") // Vide
                .amount(-100.0) // Négatif
                .build();

        // When & Then
        mockMvc.perform(post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}

