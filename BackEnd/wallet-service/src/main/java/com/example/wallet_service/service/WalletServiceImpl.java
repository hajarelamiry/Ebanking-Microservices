package com.example.wallet_service.service;

import com.example.wallet_service.account.AccountClient;
import com.example.wallet_service.dto.*;
import com.example.wallet_service.entity.Expense;
import com.example.wallet_service.entity.Wallet;
import com.example.wallet_service.exception.*;
import com.example.wallet_service.repository.ExpenseRepository;
import com.example.wallet_service.repository.WalletRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final ExpenseRepository expenseRepository;
    private final AccountClient accountClient;

    @Override
    @Transactional
    public WalletResponseDto createWallet(
            WalletRequestDto request,
            String userId) {

        // Validation des données
        validateWalletRequest(request);

        // Vérifier que le compte existe (token propagé automatiquement)
        try {
            accountClient.getSolde(request.getAccountRef());
        } catch (Exception e) {
            log.error("Compte bancaire {} introuvable", request.getAccountRef());
            throw new AccountNotFoundException("Le compte bancaire spécifié n'existe pas");
        }

        Wallet wallet = Wallet.builder()
                .name(request.getName())
                .userId(userId)
                .accountRef(request.getAccountRef())
                .budgetLimit(request.getBudgetLimit())
                .build();

        Wallet savedWallet = walletRepository.save(wallet);
        log.info("Wallet créé: {} pour user: {}", savedWallet.getWalletRef(), userId);

        return mapToWalletDto(savedWallet);
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE) // ✅ Évite les race conditions
    public ExpenseResponseDto addExpense(
            String walletRef,
            ExpenseRequestDto request,
            String userId) {

        // Validation des données
        validateExpenseRequest(request);

        // Récupérer le wallet avec un lock pessimiste
        Wallet wallet = walletRepository.findByWalletRefForUpdate(walletRef)
                .orElseThrow(() -> new WalletNotFoundException("Wallet " + walletRef + " introuvable"));

        // Vérifier l'accès
        if (!wallet.getUserId().equals(userId)) {
            log.warn("Tentative d'accès non autorisé au wallet {} par user {}", walletRef, userId);
            throw new UnauthorizedAccessException("Vous n'avez pas accès à ce wallet");
        }

        // Calculer le total dépensé
        BigDecimal spent = expenseRepository.sumAmountByWallet(wallet.getId());
        if (spent == null) {
            spent = BigDecimal.ZERO;
        }

        // Vérifier le budget
        BigDecimal newTotal = spent.add(request.getAmount());
        if (newTotal.compareTo(wallet.getBudgetLimit()) > 0) {
            log.warn("Budget dépassé pour wallet {}: {} > {}",
                    walletRef, newTotal, wallet.getBudgetLimit());
            throw new BudgetExceededException(
                    String.format("Budget dépassé. Limite: %.2f€, Déjà dépensé: %.2f€, Tentative: %.2f€",
                            wallet.getBudgetLimit(), spent, request.getAmount())
            );
        }

        // 1. D'ABORD débiter le compte bancaire
        try {
            Map<String, BigDecimal> payload = Map.of("amount", request.getAmount());
            accountClient.debitAccount(wallet.getAccountRef(), payload);
            log.info("Compte {} débité de {}€", wallet.getAccountRef(), request.getAmount());
        } catch (Exception e) {
            log.error("Échec du débit bancaire pour wallet {}: {}", walletRef, e.getMessage());
            throw new PaymentFailedException("Échec du paiement: " + e.getMessage());
        }

        // 2. PUIS enregistrer l'expense (seulement si débit OK)
        Expense expense = Expense.builder()
                .wallet(wallet)
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .date(LocalDateTime.now())
                .build();

        Expense savedExpense = expenseRepository.save(expense);
        log.info("Dépense ajoutée au wallet {}: {}€ - {}", walletRef, request.getAmount(), request.getDescription());

        return mapToExpenseDto(savedExpense);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletSummaryDto getGlobalStatus(String walletRef, String userId) {

        Wallet wallet = walletRepository.findByWalletRef(walletRef)
                .orElseThrow(() -> new WalletNotFoundException("Wallet " + walletRef + " introuvable"));

        // Vérifier l'accès
        if (!wallet.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException("Vous n'avez pas accès à ce wallet");
        }

        // Calculer le total dépensé
        BigDecimal totalSpent = expenseRepository.sumAmountByWallet(wallet.getId());
        if (totalSpent == null) {
            totalSpent = BigDecimal.ZERO;
        }

        BigDecimal remainingBudget = wallet.getBudgetLimit().subtract(totalSpent);
        boolean isOverBudget = totalSpent.compareTo(wallet.getBudgetLimit()) > 0;

        return WalletSummaryDto.builder()
                .walletName(wallet.getName())
                .walletRef(wallet.getWalletRef())
                .budgetLimit(wallet.getBudgetLimit())
                .totalSpent(totalSpent)
                .remainingBudget(remainingBudget)
                .isOverBudget(isOverBudget)
                .expenses(
                        wallet.getExpenses()
                                .stream()
                                .map(this::mapToExpenseDto)
                                .toList()
                )
                .build();
    }

    // ========== VALIDATIONS ==========

    private void validateWalletRequest(WalletRequestDto request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new InvalidInputException("Le nom du wallet est obligatoire");
        }

        if (request.getAccountRef() == null || request.getAccountRef().trim().isEmpty()) {
            throw new InvalidInputException("La référence du compte est obligatoire");
        }

        if (request.getBudgetLimit() == null || request.getBudgetLimit().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Le budget doit être supérieur à 0");
        }
    }

    private void validateExpenseRequest(ExpenseRequestDto request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidInputException("Le montant doit être supérieur à 0");
        }

        if (request.getCategory() == null || request.getCategory().trim().isEmpty()) {
            throw new InvalidInputException("La catégorie est obligatoire");
        }

        if (request.getDescription() == null || request.getDescription().trim().isEmpty()) {
            throw new InvalidInputException("La description est obligatoire");
        }
    }

    // ========== MAPPERS ==========

    private WalletResponseDto mapToWalletDto(Wallet wallet) {
        return WalletResponseDto.builder()
                .walletRef(wallet.getWalletRef())
                .name(wallet.getName())
                .budgetLimit(wallet.getBudgetLimit())
                .accountRef(wallet.getAccountRef())
                .build();
    }

    private ExpenseResponseDto mapToExpenseDto(Expense expense) {
        return ExpenseResponseDto.builder()
                .amount(expense.getAmount())
                .category(expense.getCategory())
                .description(expense.getDescription())
                .date(expense.getDate())
                .build();
    }
}