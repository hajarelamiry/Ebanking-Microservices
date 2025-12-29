package com.example.payment_service.service;

import com.example.payment_service.dto.PaymentRequestDTO;
import com.example.payment_service.dto.PaymentResponseDTO;

/**
 * Interface du service de gestion des paiements
 */
public interface PaymentService {

    /**
     * Initie un nouveau virement
     * 
     * @param requestDTO La requête de virement
     * @return La réponse avec les détails de la transaction créée
     */
    PaymentResponseDTO initiatePayment(PaymentRequestDTO requestDTO);
}
