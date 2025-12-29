package com.example.payment_service.service.fraud;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Stratégie de détection de fraude : Vérification de la vélocité des transactions
 * Détecte si trop de transferts sont effectués en moins de 10 minutes
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VelocityFraudStrategy implements FraudStrategy {

    private final TransactionRepository transactionRepository;
    private static final int VELOCITY_WINDOW_MINUTES = 10;
    private static final int MAX_TRANSACTIONS_IN_WINDOW = 5;
    private String violatedRule;

    @Override
    public double evaluateRisk(Transaction transaction, PaymentRequest request) {
        violatedRule = null;

        // Calculer la fenêtre de temps (10 minutes avant maintenant)
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);

        // Compter les transactions récentes pour ce compte
        List<com.example.payment_service.model.Transaction> recentTransactions = 
                transactionRepository.findBySourceAccountId(request.getSourceAccountId())
                        .stream()
                        .filter(t -> t.getCreatedAt().isAfter(windowStart))
                        .filter(t -> t.getStatus() != com.example.payment_service.enums.TransactionStatus.REJECTED)
                        .toList();

        int transactionCount = recentTransactions.size();

        if (transactionCount >= MAX_TRANSACTIONS_IN_WINDOW) {
            violatedRule = "VELOCITY_THRESHOLD_EXCEEDED";
            log.warn("Trop de transactions en {} minutes pour le compte {}: {} transactions", 
                    VELOCITY_WINDOW_MINUTES, request.getSourceAccountId(), transactionCount);
            return 90.0; // Score très élevé
        }

        // Score basé sur le nombre de transactions
        if (transactionCount >= MAX_TRANSACTIONS_IN_WINDOW - 1) {
            return 50.0; // Risque modéré à élevé
        } else if (transactionCount >= MAX_TRANSACTIONS_IN_WINDOW - 2) {
            return 25.0; // Risque faible à modéré
        }

        return 0.0; // Aucun risque
    }

    @Override
    public String getViolatedRule() {
        return violatedRule;
    }

    @Override
    public String getStrategyName() {
        return "VELOCITY_CHECK";
    }
}

