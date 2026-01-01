package com.example.demo.integration;

import com.example.demo.enums.CryptoSymbol;
import com.example.demo.enums.TradeType;
import com.example.demo.model.CryptoTransaction;
import com.example.demo.model.CryptoWallet;
import com.example.demo.repository.CryptoTransactionRepository;
import com.example.demo.repository.CryptoWalletRepository;
import com.example.demo.service.AuditService;
import com.example.demo.service.CryptoTradingService;
import com.example.demo.service.CryptoPriceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests d'intégration pour Crypto Service
 * Teste le flux complet avec base de données réelle
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CryptoIntegrationTest {

    @Autowired
    private CryptoTradingService tradingService;

    @Autowired
    private CryptoWalletRepository walletRepository;

    @Autowired
    private CryptoTransactionRepository transactionRepository;

    @MockBean
    private CryptoPriceService priceService;

    @MockBean
    private AuditService auditService; // Mock pour éviter l'appel réel à audit-service

    @BeforeEach
    void setUp() {
        // Nettoyer la base de données avant chaque test
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void shouldCreateBuyTransaction() {
        // Given
        Long userId = 10L; // Utiliser un userId unique
        CryptoSymbol symbol = CryptoSymbol.BTC;
        Double quantity = 0.5;
        Double price = 50000.0;

        // S'assurer qu'il n'y a pas de wallet existant
        walletRepository.findByUserIdAndSymbol(userId, symbol)
                .ifPresent(w -> walletRepository.delete(w));

        when(priceService.getCryptoPrice(symbol)).thenReturn(price);

        // When
        CryptoTransaction transaction = tradingService.trade(userId, symbol, quantity, TradeType.BUY);

        // Then
        assertNotNull(transaction);
        assertNotNull(transaction.getId());
        assertEquals(userId, transaction.getUserId());
        assertEquals(symbol, transaction.getSymbol());
        assertEquals(TradeType.BUY, transaction.getType());
        assertEquals(quantity, transaction.getQuantity());
        assertEquals(price, transaction.getPriceAtTime());

        // Vérifier que le wallet a été créé/mis à jour
        CryptoWallet wallet = walletRepository.findByUserIdAndSymbol(userId, symbol).orElse(null);
        assertNotNull(wallet);
        assertEquals(quantity, wallet.getBalance(), 0.001);

        // Vérifier que l'audit a été appelé
        verify(auditService).sendAuditEvent(any());
    }

    @Test
    void shouldCreateSellTransaction() {
        // Given
        Long userId = 2L; // Utiliser un userId différent pour éviter les conflits
        CryptoSymbol symbol = CryptoSymbol.ETH;
        Double initialBalance = 2.0;
        Double sellQuantity = 0.5;
        Double price = 3000.0;

        // Créer un wallet avec un solde initial
        CryptoWallet wallet = CryptoWallet.builder()
                .userId(userId)
                .symbol(symbol)
                .balance(initialBalance)
                .build();
        walletRepository.save(wallet);

        when(priceService.getCryptoPrice(symbol)).thenReturn(price);

        // When
        CryptoTransaction transaction = tradingService.trade(userId, symbol, sellQuantity, TradeType.SELL);

        // Then
        assertNotNull(transaction);
        assertEquals(TradeType.SELL, transaction.getType());
        assertEquals(sellQuantity, transaction.getQuantity());

        // Vérifier que le solde a été débité
        CryptoWallet updatedWallet = walletRepository.findByUserIdAndSymbol(userId, symbol).orElse(null);
        assertNotNull(updatedWallet);
        assertEquals(initialBalance - sellQuantity, updatedWallet.getBalance());

        // Vérifier que l'audit a été appelé
        verify(auditService).sendAuditEvent(any());
    }

    @Test
    void shouldThrowExceptionWhenInsufficientBalance() {
        // Given
        Long userId = 3L; // Utiliser un userId différent
        CryptoSymbol symbol = CryptoSymbol.BTC;
        Double quantity = 1.0;
        Double price = 50000.0;

        // S'assurer qu'il n'y a pas de wallet pour cet utilisateur
        walletRepository.deleteAll();

        when(priceService.getCryptoPrice(symbol)).thenReturn(price);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tradingService.trade(userId, symbol, quantity, TradeType.SELL)
        );

        assertTrue(exception.getMessage().contains("Insufficient balance"));
        verify(auditService, never()).sendAuditEvent(any());
    }
}
