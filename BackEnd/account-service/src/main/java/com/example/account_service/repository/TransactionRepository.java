package com.example.account_service.repository;

import com.example.account_service.entity.Account;
import com.example.account_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByDestinateurOrDestinataireAndDateHeureBetween(
            Account sender,
            Account receiver,
            LocalDateTime start,
            LocalDateTime end
    );
}
