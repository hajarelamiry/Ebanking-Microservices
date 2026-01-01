package com.example.demo.service;

import com.example.demo.dto.AuditEventDTO;
import com.example.demo.enums.CryptoSymbol;
import com.example.demo.enums.TradeType;
import com.example.demo.exception.InsufficientBalanceException;
import com.example.demo.exception.ServiceUnavailableException;
import com.example.demo.model.CryptoTransaction;
import com.example.demo.model.CryptoWallet;
import com.example.demo.repository.CryptoTransactionRepository;
import com.example.demo.repository.CryptoWalletRepository;
import com.example.demo.util.CorrelationIdContext;
import com.example.demo.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CryptoTradingService {
    
    private final CryptoWalletRepository walletRepository;
    private final CryptoTransactionRepository transactionRepository;
    private final CryptoPriceService priceService;
    private final AuditService auditService; // Communication synchrone via Eureka/Feign
    private final AccountService accountService; // Communication avec Account Service via Feign
    
    @Transactional
    public CryptoTransaction trade(Long userId, CryptoSymbol symbol, Double quantity, TradeType type) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }
        
        Double currentPrice = priceService.getCryptoPrice(symbol);
        if (currentPrice == null) {
            throw new IllegalStateException("Prix non disponible pour " + symbol);
        }
        
        // Récupérer l'accountRef depuis account-service via le nouvel endpoint
        // Utiliser le preferred_username du JWT car account-service attend un username (ex: "user1")
        String username = JwtUtils.getUsername();
        if (username == null || username.isEmpty()) {
            throw new IllegalStateException("Impossible de récupérer le username depuis le JWT. Le token ne contient pas de preferred_username.");
        }
        
        String accountRef;
        try {
            accountRef = accountService.getAccountRefByUserId(username);
            log.debug("Account reference retrieved for username {}: {}", username, accountRef);
        } catch (RuntimeException e) {
            throw new ServiceUnavailableException("account-service", 
                    "Impossible de récupérer le compte bancaire de l'utilisateur: " + e.getMessage(), e);
        }
        
        CryptoWallet wallet = walletRepository.findByUserIdAndSymbol(userId, symbol)
                .orElse(CryptoWallet.builder()
                        .userId(userId)
                        .symbol(symbol)
                        .balance(0.0)
                        .build());
        
        if (type == TradeType.BUY) {
            // Pour un achat : vérifier le solde bancaire, débiter le compte, créditer le wallet crypto
            BigDecimal totalAmount = BigDecimal.valueOf(quantity * currentPrice);
            
            // Vérifier le solde du compte bancaire
            BigDecimal balance;
            try {
                balance = accountService.getBalance(accountRef);
                if (balance.compareTo(totalAmount) < 0) {
                    throw new InsufficientBalanceException(
                            String.format("Solde bancaire insuffisant pour effectuer l'achat. Solde disponible: %s EUR, Montant requis: %s EUR", 
                                    balance, totalAmount),
                            totalAmount,
                            balance);
                }
            } catch (RuntimeException e) {
                if (e instanceof InsufficientBalanceException) {
                    throw e;
                }
                throw new ServiceUnavailableException("account-service", 
                        "Impossible de vérifier le solde du compte bancaire: " + e.getMessage(), e);
            }
            
            // Débiter le compte bancaire
            try {
                accountService.debitAccount(accountRef, totalAmount);
                log.info("Account {} debited with {} EUR for crypto purchase", accountRef, totalAmount);
            } catch (RuntimeException e) {
                throw new ServiceUnavailableException("account-service", 
                        "Impossible de débiter le compte bancaire: " + e.getMessage(), e);
            }
            
            // Créditer le wallet crypto
            wallet.setBalance(wallet.getBalance() + quantity);
            log.info("User {} bought {} {} at {} EUR (Total: {} EUR)", userId, quantity, symbol, currentPrice, totalAmount);
            
        } else if (type == TradeType.SELL) {
            // Pour une vente : vérifier le solde crypto, débiter le wallet crypto, créditer le compte bancaire
            if (wallet.getBalance() < quantity) {
                throw new InsufficientBalanceException(
                        String.format("Solde crypto insuffisant. Disponible: %s %s, Demandé: %s %s", 
                                wallet.getBalance(), symbol, quantity, symbol),
                        BigDecimal.valueOf(quantity),
                        BigDecimal.valueOf(wallet.getBalance()));
            }
            
            // Débiter le wallet crypto
            wallet.setBalance(wallet.getBalance() - quantity);
            
            // Calculer le montant en EUR à créditer
            BigDecimal totalAmount = BigDecimal.valueOf(quantity * currentPrice);
            
            // Créditer le compte bancaire
            try {
                accountService.creditAccount(accountRef, totalAmount);
                log.info("Account {} credited with {} EUR from crypto sale", accountRef, totalAmount);
            } catch (RuntimeException e) {
                throw new ServiceUnavailableException("account-service", 
                        "Impossible de créditer le compte bancaire: " + e.getMessage(), e);
            }
            
            log.info("User {} sold {} {} at {} EUR (Total: {} EUR)", userId, quantity, symbol, currentPrice, totalAmount);
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
        
        // Publication de l'événement d'audit via Feign Client (synchrone)
        sendAuditEventForTrade(transaction, type);
        
        return transaction;
    }

    /**
     * Envoie un événement d'audit pour un trade crypto via Feign Client
     */
    private void sendAuditEventForTrade(CryptoTransaction transaction, TradeType tradeType) {
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
        
        // Envoie via Feign Client (Eureka découvre automatiquement audit-service)
        auditService.sendAuditEvent(auditEvent);
    }
}

