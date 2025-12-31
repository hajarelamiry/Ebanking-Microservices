package com.example.demo.repository;

import com.example.demo.enums.CryptoSymbol;
import com.example.demo.model.CryptoWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CryptoWalletRepository extends JpaRepository<CryptoWallet, Long> {
    
    Optional<CryptoWallet> findByUserIdAndSymbol(Long userId, CryptoSymbol symbol);
    
    List<CryptoWallet> findByUserId(Long userId);
}

