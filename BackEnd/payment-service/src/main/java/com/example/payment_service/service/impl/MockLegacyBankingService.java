package com.example.payment_service.service.impl;

import com.example.payment_service.dto.LegacyPaymentRequest;
import com.example.payment_service.dto.LegacyPaymentResponse;
import com.example.payment_service.service.LegacyBankingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Implémentation MOCK pour le développement isolé
 * Simule les appels SOAP au système legacy sans appeler le service réel
 * 
 * Activée par défaut ou si payment.mock.enabled=true
 */
@Service
@ConditionalOnProperty(name = "payment.mock.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockLegacyBankingService implements LegacyBankingService {

    @Override
    public LegacyPaymentResponse sendPayment(LegacyPaymentRequest request) {
        log.info("🟡 [MOCK] ═══════════════════════════════════════════════════════");
        log.info("🟡 [MOCK] Appel SOAP simulé pour un virement de {} {}", 
                request.getAmount(), request.getCurrency());
        log.info("🟡 [MOCK] Détails du virement simulé:");
        log.info("   - Transaction ID: {}", request.getTransactionId());
        log.info("   - Compte source: {}", request.getSourceAccountId());
        log.info("   - IBAN destination: {}", request.getDestinationIban());
        log.info("   - Montant: {} {}", request.getAmount(), request.getCurrency());
        log.info("🟡 [MOCK] ═══════════════════════════════════════════════════════");
        
        // Simulation : 95% de succès, 5% d'échec pour tester la compensation
        boolean success = Math.random() > 0.05;
        
        if (success) {
            String legacyReference = "LEG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            log.info("🟡 [MOCK] ✅ Virement simulé avec SUCCÈS");
            log.info("🟡 [MOCK] Référence Legacy générée: {}", legacyReference);
            
            return LegacyPaymentResponse.builder()
                    .transactionId(request.getTransactionId())
                    .success(true)
                    .message("Virement simulé traité avec succès")
                    .legacyReference(legacyReference)
                    .build();
        } else {
            log.warn("🟡 [MOCK] ❌ Virement simulé ÉCHOUÉ (simulation d'erreur pour tester la compensation)");
            
            return LegacyPaymentResponse.builder()
                    .transactionId(request.getTransactionId())
                    .success(false)
                    .message("Erreur simulée du système legacy (test de compensation)")
                    .legacyReference(null)
                    .build();
        }
    }
}

