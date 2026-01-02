package com.example.analyticsservice.controller;

// Corrected imports to match your package name
import com.example.analyticsservice.model.Alert;
import com.example.analyticsservice.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/total-expenses-by-category/{category}")
    public ResponseEntity<Double> getTotalExpensesByCategory(@PathVariable String category) {
        Double total = analyticsService.getTotalExpensesByCategory(category);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/check-budget/{portefeuilleId}/{budgetLimit}")
    public ResponseEntity<Alert> checkBudgetExceeded(@PathVariable String portefeuilleId, @PathVariable Double budgetLimit) {
        Alert alert = analyticsService.checkBudgetExceeded(portefeuilleId, budgetLimit);
        return ResponseEntity.ok(alert);
    }
}