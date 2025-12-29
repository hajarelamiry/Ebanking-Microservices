package com.example.payment_service.service.fraud;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.repositories.BlacklistedIbanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stratégie de détection de fraude : Vérification des listes noires (IBAN)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BlacklistFraudStrategy implements FraudStrategy {

    private final BlacklistedIbanRepository blacklistedIbanRepository;
    private String violatedRule;

    @Override
    public double evaluateRisk(Transaction transaction, PaymentRequest request) {
        violatedRule = null;
        
        // Vérifier si l'IBAN de destination est dans la liste noire
        boolean isBlacklisted = blacklistedIbanRepository.existsByIbanAndIsActiveTrue(
                request.getDestinationIban()
        );

        if (isBlacklisted) {
            violatedRule = "IBAN_DESTINATION_BLACKLISTED";
            log.warn("IBAN de destination {} est dans la liste noire pour la transaction {}", 
                    request.getDestinationIban(), transaction.getId());
            return 100.0; // Score maximum = rejet automatique
        }

        return 0.0; // Aucun risque
    }

    @Override
    public String getViolatedRule() {
        return violatedRule;
    }

    @Override
    public String getStrategyName() {
        return "BLACKLIST_CHECK";
    }
}

