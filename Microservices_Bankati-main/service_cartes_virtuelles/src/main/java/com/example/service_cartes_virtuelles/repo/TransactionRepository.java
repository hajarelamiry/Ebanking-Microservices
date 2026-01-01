package com.example.service_cartes_virtuelles.repo;

import org.example.dto.TransactionDTO;
import org.example.entites.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction , Long> {
    List<Transaction> findByCarteVirtuelleId(Long carteVirtuelleId);
}
