package com.example.payment_service.service.fraud;

import com.example.payment_service.dto.PaymentRequest;
import com.example.payment_service.model.Transaction;
import com.example.payment_service.repositories.AccountLimitRepository;
import com.example.payment_service.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Stratégie de détection de fraude : Vérification des plafonds journaliers et mensuels
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LimitFraudStrategy implements FraudStrategy {

    private final AccountLimitRepository accountLimitRepository;
    private final TransactionRepository transactionRepository;
    private String violatedRule;

    @Override
    public double evaluateRisk(Transaction transaction, PaymentRequest request) {
        violatedRule = null;

        // Récupérer les plafonds du compte
        var accountLimit = accountLimitRepository.findByAccountIdAndCurrency(
                request.getSourceAccountId(),
                request.getCurrency()
        );

        if (accountLimit.isEmpty()) {
            log.debug("Aucun plafond défini pour le compte {}", request.getSourceAccountId());
            return 0.0; // Pas de plafond = pas de restriction
        }

        var limit = accountLimit.get();
        BigDecimal transactionAmount = request.getAmount();

        // Vérifier le plafond journalier
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        BigDecimal dailyTotal = transactionRepository
                .findBySourceAccountId(request.getSourceAccountId())
                .stream()
                .filter(t -> t.getCreatedAt().isAfter(startOfDay) 
                        && t.getCurrency().equals(request.getCurrency())
                        && (t.getStatus() == com.example.payment_service.enums.TransactionStatus.VALIDATED
                                || t.getStatus() == com.example.payment_service.enums.TransactionStatus.COMPLETED))
                .map(com.example.payment_service.model.Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal dailyTotalWithNewTransaction = dailyTotal.add(transactionAmount);
        if (dailyTotalWithNewTransaction.compareTo(limit.getDailyLimit()) > 0) {
            violatedRule = "DAILY_LIMIT_EXCEEDED";
            log.warn("Plafond journalier dépassé pour le compte {}: {} > {}", 
                    request.getSourceAccountId(), dailyTotalWithNewTransaction, limit.getDailyLimit());
            return 80.0; // Score élevé
        }

        // Vérifier le plafond mensuel
        LocalDateTime startOfMonth = LocalDateTime.of(
                LocalDate.now().withDayOfMonth(1), 
                LocalTime.MIN
        );
        BigDecimal monthlyTotal = transactionRepository
                .findBySourceAccountId(request.getSourceAccountId())
                .stream()
                .filter(t -> t.getCreatedAt().isAfter(startOfMonth) 
                        && t.getCurrency().equals(request.getCurrency())
                        && (t.getStatus() == com.example.payment_service.enums.TransactionStatus.VALIDATED
                                || t.getStatus() == com.example.payment_service.enums.TransactionStatus.COMPLETED))
                .map(com.example.payment_service.model.Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyTotalWithNewTransaction = monthlyTotal.add(transactionAmount);
        if (monthlyTotalWithNewTransaction.compareTo(limit.getMonthlyLimit()) > 0) {
            violatedRule = "MONTHLY_LIMIT_EXCEEDED";
            log.warn("Plafond mensuel dépassé pour le compte {}: {} > {}", 
                    request.getSourceAccountId(), monthlyTotalWithNewTransaction, limit.getMonthlyLimit());
            return 75.0; // Score élevé
        }

        // Calculer un score basé sur le pourcentage d'utilisation
        double dailyUsagePercent = dailyTotalWithNewTransaction
                .divide(limit.getDailyLimit(), 2, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        if (dailyUsagePercent > 90) {
            return 30.0; // Risque modéré si proche de la limite
        } else if (dailyUsagePercent > 70) {
            return 15.0; // Risque faible mais surveillé
        }

        return 0.0; // Aucun risque
    }

    @Override
    public String getViolatedRule() {
        return violatedRule;
    }

    @Override
    public String getStrategyName() {
        return "LIMIT_CHECK";
    }
}

