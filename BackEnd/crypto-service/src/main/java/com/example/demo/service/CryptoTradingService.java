package com.example.demo.service;

import com.example.demo.enums.CryptoSymbol;
import com.example.demo.enums.TradeType;
import com.example.demo.model.CryptoTransaction;
import com.example.demo.model.CryptoWallet;
import com.example.demo.repository.CryptoTransactionRepository;
import com.example.demo.repository.CryptoWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Slf4j
public class CryptoTradingService {
    
    private final CryptoWalletRepository walletRepository;
    private final CryptoTransactionRepository transactionRepository;
    private final CryptoPriceService priceService;
    
    @Transactional
    public CryptoTransaction trade(Long userId, CryptoSymbol symbol, Double quantity, TradeType type) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        Double currentPrice = priceService.getCryptoPrice(symbol);
        if (currentPrice == null) {
            throw new IllegalStateException("Price not available for " + symbol);
        }
        
        CryptoWallet wallet = walletRepository.findByUserIdAndSymbol(userId, symbol)
                .orElse(CryptoWallet.builder()
                        .userId(userId)
                        .symbol(symbol)
                        .balance(0.0)
                        .build());
        
        if (type == TradeType.BUY) {
            wallet.setBalance(wallet.getBalance() + quantity);
            log.info("User {} bought {} {} at {} EUR", userId, quantity, symbol, currentPrice);
        } else if (type == TradeType.SELL) {
            if (wallet.getBalance() < quantity) {
                throw new IllegalArgumentException(
                        String.format("Insufficient balance. Available: %s, Requested: %s", 
                                wallet.getBalance(), quantity));
            }
            wallet.setBalance(wallet.getBalance() - quantity);
            log.info("User {} sold {} {} at {} EUR", userId, quantity, symbol, currentPrice);
        }
        
        walletRepository.save(wallet);
        
        CryptoTransaction transaction = CryptoTransaction.builder()
                .userId(userId)
                .symbol(symbol)
                .type(type)
                .quantity(quantity)
                .priceAtTime(currentPrice)
                .build();
        
        return transactionRepository.save(transaction);
    }
}

