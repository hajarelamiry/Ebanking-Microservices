package com.example.wallet_service.service;

import com.example.wallet_service.account.AccountClient;
import com.example.wallet_service.dto.*;
import com.example.wallet_service.entity.Expense;
import com.example.wallet_service.entity.Wallet;
import com.example.wallet_service.repository.ExpenseRepository;
import com.example.wallet_service.repository.WalletRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final ExpenseRepository expenseRepository;
    private final AccountClient accountClient;

    @Override
    public WalletResponseDto createWallet(
            WalletRequestDto request,
            String userId) {

        // Vérifier que le compte existe (token propagé automatiquement)
        accountClient.getSolde(request.getAccountRef());

        Wallet wallet = Wallet.builder()
                .name(request.getName())
                .userId(userId)
                .accountRef(request.getAccountRef())
                .budgetLimit(request.getBudgetLimit())
                .build();

        return mapToWalletDto(walletRepository.save(wallet));
    }

    @Override
    public ExpenseResponseDto addExpense(
            String walletRef,
            ExpenseRequestDto request,
            String userId) {

        Wallet wallet = walletRepository.findByWalletRef(walletRef)
                .filter(w -> w.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Wallet introuvable ou accès refusé"));

        BigDecimal spent = expenseRepository.sumAmountByWallet(wallet.getId());
        if (spent == null) spent = BigDecimal.ZERO;

        if (spent.add(request.getAmount()).compareTo(wallet.getBudgetLimit()) > 0) {
            throw new RuntimeException("Budget du wallet dépassé (" + wallet.getBudgetLimit() + " max)");
        }

        // Débit bancaire (Authorization propagé)
        Map<String, BigDecimal> payload = Map.of("amount", request.getAmount());
        accountClient.debitAccount(wallet.getAccountRef(), payload);

        Expense expense = Expense.builder()
                .wallet(wallet)
                .amount(request.getAmount())
                .category(request.getCategory())
                .description(request.getDescription())
                .date(LocalDateTime.now())
                .build();

        return mapToExpenseDto(expenseRepository.save(expense));
    }

    @Override
    @Transactional(readOnly = true)
    public WalletSummaryDto getGlobalStatus(String walletRef, String userId) {

        Wallet wallet = walletRepository.findByWalletRef(walletRef)
                .filter(w -> w.getUserId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Accès interdit"));

        BigDecimal totalSpent = expenseRepository.sumAmountByWallet(wallet.getId());
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        return WalletSummaryDto.builder()
                .walletName(wallet.getName())
                .walletRef(wallet.getWalletRef())
                .budgetLimit(wallet.getBudgetLimit())
                .totalSpent(totalSpent)
                .remainingBudget(wallet.getBudgetLimit().subtract(totalSpent))
                .isOverBudget(totalSpent.compareTo(wallet.getBudgetLimit()) > 0)
                .expenses(
                        wallet.getExpenses()
                                .stream()
                                .map(this::mapToExpenseDto)
                                .toList()
                )
                .build();
    }

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
