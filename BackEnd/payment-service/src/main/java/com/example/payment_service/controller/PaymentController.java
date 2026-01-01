package com.example.payment_service.controller;

import com.example.payment_service.dto.PaymentRequestDTO;
import com.example.payment_service.dto.PaymentResponseDTO;
import com.example.payment_service.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour les paiements
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "API de gestion des virements")
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Crée un nouveau virement
     */
    @PostMapping
    @Operation(
        summary = "Créer un virement",
        description = "Initie un nouveau virement entre deux comptes"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Virement créé avec succès"),
        @ApiResponse(responseCode = "400", description = "Requête invalide"),
        @ApiResponse(responseCode = "422", description = "Transaction rejetée (règle anti-fraude)")
    })
    public ResponseEntity<PaymentResponseDTO> createPayment(@Valid @RequestBody PaymentRequestDTO requestDTO) {
        log.info("Requête de création de virement reçue: compte={}, montant={}", 
                requestDTO.getSourceAccountId(), requestDTO.getAmount());
        
        PaymentResponseDTO response = paymentService.initiatePayment(requestDTO);
        
        HttpStatus status = switch (response.getStatus()) {
            case VALIDATED, COMPLETED -> HttpStatus.CREATED;
            case REJECTED -> HttpStatus.UNPROCESSABLE_ENTITY;
            case PENDING_MANUAL_REVIEW -> HttpStatus.ACCEPTED; // 202 Accepted pour validation manuelle
            default -> HttpStatus.ACCEPTED;
        };
        
        return ResponseEntity.status(status).body(response);
    }
}
