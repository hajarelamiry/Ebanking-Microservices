package com.example.payment_service.controller;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.dto.PaymentResponse;
import com.example.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour les virements
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Crée et traite un nouveau virement
     */
    @PostMapping("/transfer")
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Requête de virement reçue: type={}, montant={}, compte={}", 
                request.getType(), request.getAmount(), request.getSourceAccountId());
        
        PaymentResponse response = paymentService.processPayment(request);
        
        HttpStatus status = switch (response.getStatus()) {
            case VALIDATED -> HttpStatus.ACCEPTED;
            case COMPLETED -> HttpStatus.OK;
            case REJECTED -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.PROCESSING;
        };
        
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Récupère le statut d'une transaction
     */
    @GetMapping("/{transactionId}/status")
    public ResponseEntity<PaymentResponse> getTransactionStatus(@PathVariable String transactionId) {
        // TODO: Implémenter la récupération du statut
        return ResponseEntity.notFound().build();
    }
}

