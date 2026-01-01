package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.enums.CryptoSymbol;
import com.example.demo.model.CryptoTransaction;
import com.example.demo.model.CryptoWallet;
import com.example.demo.repository.CryptoTransactionRepository;
import com.example.demo.repository.CryptoWalletRepository;
import com.example.demo.service.CryptoPriceService;
import com.example.demo.service.CryptoTradingService;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.util.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/crypto")
@RequiredArgsConstructor
@Slf4j
public class CryptoController {
    
    private final CryptoPriceService priceService;
    private final CryptoTradingService tradingService;
    private final CryptoWalletRepository walletRepository;
    private final CryptoTransactionRepository transactionRepository;
    
    @GetMapping("/prices")
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN')")
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
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN')")
    public ResponseEntity<WalletResponse> getWallet(@RequestParam(required = false) Long userId) {
        // Si userId n'est pas fourni, utiliser celui du token
        Long targetUserId = userId;
        if (targetUserId == null) {
            targetUserId = JwtUtils.getUserIdAsLong();
            if (targetUserId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }
        
        // Vérifier que le CLIENT ne peut accéder qu'à son propre wallet
        if (JwtUtils.isClient() && !targetUserId.equals(JwtUtils.getUserIdAsLong())) {
            throw new AccessDeniedException("CLIENT can only access their own wallet");
        }
        
        List<CryptoWallet> wallets = walletRepository.findByUserId(targetUserId);
        
        List<WalletResponse.WalletItem> walletItems = wallets.stream()
                .map(wallet -> WalletResponse.WalletItem.builder()
                        .symbol(wallet.getSymbol().name())
                        .balance(wallet.getBalance())
                        .build())
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(WalletResponse.builder()
                .userId(targetUserId)
                .wallets(walletItems)
                .build());
    }
    
    @PostMapping("/trade")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<TransactionResponse> trade(
            @RequestParam(required = false) Long userId,
            @Valid @RequestBody TradeRequest request) {
        
        // Utiliser le userId du token si non fourni
        Long targetUserId = userId;
        if (targetUserId == null) {
            // Récupérer directement depuis le JWT (sans appeler user-service)
            targetUserId = JwtUtils.getUserIdAsLong();
            log.debug("getUserIdAsLong() returned: {}", targetUserId);
            
            if (targetUserId == null) {
                String jwtUserId = JwtUtils.getUserId();
                log.error("Unable to determine targetUserId from JWT. JWT userId: {}", jwtUserId);
                throw new UserNotFoundException(
                        "Impossible de déterminer l'identifiant utilisateur depuis le JWT. Le token ne contient pas d'identifiant valide.");
            }
        }
        
        log.debug("Using targetUserId: {}", targetUserId);
        
        // Vérifier que le CLIENT ne peut trader que pour lui-même
        Long currentUserId = JwtUtils.getUserIdAsLong();
        if (currentUserId != null && !targetUserId.equals(currentUserId)) {
            throw new AccessDeniedException("CLIENT can only trade for themselves");
        }
        
        // Appeler le service - les exceptions seront gérées par GlobalExceptionHandler
        CryptoTransaction transaction = tradingService.trade(
                targetUserId,
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
    @PreAuthorize("hasAnyRole('CLIENT', 'AGENT', 'ADMIN')")
    public ResponseEntity<List<TransactionResponse>> getHistory(@RequestParam(required = false) Long userId) {
        // Si userId n'est pas fourni, utiliser celui du token
        Long targetUserId = userId;
        if (targetUserId == null) {
            targetUserId = JwtUtils.getUserIdAsLong();
            if (targetUserId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }
        
        // Vérifier que le CLIENT ne peut voir que son propre historique
        if (JwtUtils.isClient() && !targetUserId.equals(JwtUtils.getUserIdAsLong())) {
            throw new AccessDeniedException("CLIENT can only access their own history");
        }
        
        List<CryptoTransaction> transactions = transactionRepository.findByUserIdOrderByTimestampDesc(targetUserId);
        
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

