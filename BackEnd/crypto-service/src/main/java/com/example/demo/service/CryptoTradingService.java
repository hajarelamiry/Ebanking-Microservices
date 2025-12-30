package com.example.demo.service;

import com.example.demo.dto.AuditEventDTO;
import com.example.demo.enums.CryptoSymbol;
import com.example.demo.enums.TradeType;
import com.example.demo.model.CryptoTransaction;
import com.example.demo.model.CryptoWallet;
import com.example.demo.repository.CryptoTransactionRepository;
import com.example.demo.repository.CryptoWalletRepository;
import com.example.demo.util.CorrelationIdContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CryptoTradingService {
    
    private final CryptoWalletRepository walletRepository;
    private final CryptoTransactionRepository transactionRepository;
    private final CryptoPriceService priceService;
    private final EventPublisher eventPublisher;
    
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
        
        transaction = transactionRepository.save(transaction);
        
        // Publication de l'événement d'audit dans la table outbox (dans la même transaction)
        publishCryptoTradeEvent(transaction, type);
        
        return transaction;
    }

    /**
     * Publie un événement d'audit lors d'un trade crypto
     */
    private void publishCryptoTradeEvent(CryptoTransaction transaction, TradeType tradeType) {
        String eventType = tradeType == TradeType.BUY ? "CRYPTO_BUY" : "CRYPTO_SELL";
        
        AuditEventDTO auditEvent = AuditEventDTO.builder()
                .correlationId(CorrelationIdContext.getCorrelationId())
                .userId(transaction.getUserId().toString())
                .actionType(eventType)
                .serviceName("crypto-service")
                .description(String.format("Crypto %s transaction: %s %s at %s EUR", 
                        tradeType.name(), transaction.getQuantity(), 
                        transaction.getSymbol(), transaction.getPriceAtTime()))
                .status("SUCCESS")
                .timestamp(LocalDateTime.now())
                .transactionId(transaction.getId())
                .symbol(transaction.getSymbol().name())
                .tradeType(transaction.getType().name())
                .quantity(transaction.getQuantity())
                .priceAtTime(transaction.getPriceAtTime())
                .build();
        
        eventPublisher.publishEvent(
                "CryptoTransaction",
                transaction.getId().toString(),
                eventType,
                auditEvent
        );
    }
}

