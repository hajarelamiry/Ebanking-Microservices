package com.example.analyticsservice.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.example.analyticsservice.model.Expense; // Fixed specific class import
import java.util.List; // Added missing import

@Repository
public interface ExpenseRepository extends MongoRepository<Expense, String> {
    List<Expense> findByCategory(String category);
    List<Expense> findByPortefeuilleId(String portefeuilleId);
}