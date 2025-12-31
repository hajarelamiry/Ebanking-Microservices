package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.enums.CryptoSymbol;
import com.example.demo.model.CryptoTransaction;
import com.example.demo.model.CryptoWallet;
import com.example.demo.repository.CryptoTransactionRepository;
import com.example.demo.repository.CryptoWalletRepository;
import com.example.demo.service.CryptoPriceService;
import com.example.demo.service.CryptoTradingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/crypto")
@RequiredArgsConstructor
public class CryptoController {
    
    private final CryptoPriceService priceService;
    private final CryptoTradingService tradingService;
    private final CryptoWalletRepository walletRepository;
    private final CryptoTransactionRepository transactionRepository;
    
    @GetMapping("/prices")
    public ResponseEntity<PriceResponse> getPrices() {
        Map<CryptoSymbol, Double> prices = priceService.getAllPrices();
        Map<String, Double> pricesMap = prices.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().name(),
                        Map.Entry::getValue
                ));
        
        return ResponseEntity.ok(PriceResponse.builder()
                .prices(pricesMap)
                .build());
    }
    
    @GetMapping("/wallet")
    public ResponseEntity<WalletResponse> getWallet(@RequestParam Long userId) {
        List<CryptoWallet> wallets = walletRepository.findByUserId(userId);
        
        List<WalletResponse.WalletItem> walletItems = wallets.stream()
                .map(wallet -> WalletResponse.WalletItem.builder()
                        .symbol(wallet.getSymbol().name())
                        .balance(wallet.getBalance())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(WalletResponse.builder()
                .userId(userId)
                .wallets(walletItems)
                .build());
    }
    
    @PostMapping("/trade")
    public ResponseEntity<TransactionResponse> trade(
            @RequestParam Long userId,
            @Valid @RequestBody TradeRequest request) {
        
        CryptoTransaction transaction = tradingService.trade(
                userId,
                request.getSymbol(),
                request.getQuantity(),
                request.getType()
        );
        
        TransactionResponse response = TransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .symbol(transaction.getSymbol().name())
                .type(transaction.getType().name())
                .quantity(transaction.getQuantity())
                .priceAtTime(transaction.getPriceAtTime())
                .timestamp(transaction.getTimestamp())
                .build();
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/history")
    public ResponseEntity<List<TransactionResponse>> getHistory(@RequestParam Long userId) {
        List<CryptoTransaction> transactions = transactionRepository.findByUserIdOrderByTimestampDesc(userId);
        
        List<TransactionResponse> responses = transactions.stream()
                .map(transaction -> TransactionResponse.builder()
                        .id(transaction.getId())
                        .userId(transaction.getUserId())
                        .symbol(transaction.getSymbol().name())
                        .type(transaction.getType().name())
                        .quantity(transaction.getQuantity())
                        .priceAtTime(transaction.getPriceAtTime())
                        .timestamp(transaction.getTimestamp())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }
}

