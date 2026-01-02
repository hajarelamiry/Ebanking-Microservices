package com.example.analyticsservice.service;

// Corrected imports
import com.example.analyticsservice.model.Alert;
import com.example.analyticsservice.model.Expense;
import com.example.analyticsservice.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate; // Added missing import
import java.util.List;

@Service
public class AnalyticsService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public Double getTotalExpensesByCategory(String category) {
        List<Expense> expenses = expenseRepository.findByCategory(category); // Optimized to use repository method
        return expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();
    }

    public Alert checkBudgetExceeded(String portefeuilleId, Double budgetLimit) {
        List<Expense> expenses = expenseRepository.findByPortefeuilleId(portefeuilleId); // Optimized
        Double totalSpent = expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        if (totalSpent > budgetLimit) {
            return new Alert("Budget dépassé", LocalDate.now(), false);
        }
        return new Alert("Budget respecté", LocalDate.now(), true);
    }
}