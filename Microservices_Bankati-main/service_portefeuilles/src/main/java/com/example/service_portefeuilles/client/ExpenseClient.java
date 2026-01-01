package com.example.service_portefeuilles.client;

import com.example.service_portefeuilles.dto.ExpenseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "service-depenses-budget", url = "http://localhost:8084/depenses_budget/expenses")
public interface ExpenseClient {

    @GetMapping("/all/{id}")
    List<ExpenseDTO> getExpensesByPortefeuille(@PathVariable Long id);

    @PutMapping("/{depenseId}/alimenter/{montant}")
    boolean alimenterDepense(@PathVariable Long depenseId, @PathVariable Double montant);
}
