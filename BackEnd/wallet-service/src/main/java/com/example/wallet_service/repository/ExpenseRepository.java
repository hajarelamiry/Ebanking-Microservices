package com.example.wallet_service.repository;

import com.example.wallet_service.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM Expense e
        WHERE e.wallet.id = :walletId
    """)
    BigDecimal sumAmountByWallet(@Param("walletId") Long walletId);
}

