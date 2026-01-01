package com.example.service_paiement_multidevises.repository;

import org.example.entites.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction , Long> {
}
