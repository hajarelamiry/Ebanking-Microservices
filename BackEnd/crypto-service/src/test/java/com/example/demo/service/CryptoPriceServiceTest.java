package com.example.demo.service;

import com.example.demo.clients.CoinGeckoClient;
import com.example.demo.enums.CryptoSymbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoPriceServiceTest {
    
    @Mock
    private CoinGeckoClient coinGeckoClient;
    
    @InjectMocks
    private CryptoPriceService cryptoPriceService;
    
    @BeforeEach
    void setUp() {
        // Mock initial prices - UN SEUL appel pour toutes les cryptos
        Map<CryptoSymbol, Double> allPrices = Map.of(
                CryptoSymbol.BTC, 50000.0,
                CryptoSymbol.ETH, 3000.0,
                CryptoSymbol.SOL, 150.0
        );
        when(coinGeckoClient.getAllCryptoPricesInEur())
                .thenReturn(Mono.just(allPrices));
    }
    
    @Test
    void testGetCryptoPrice_Success() throws InterruptedException {
        // Given
        CryptoSymbol symbol = CryptoSymbol.BTC;
        Double expectedPrice = 50000.0;
        
        // Initialize cache
        cryptoPriceService.initializePrices();
        // Wait for async operations
        Thread.sleep(200);
        
        // When
        Double result = cryptoPriceService.getCryptoPrice(symbol);
        
        // Then
        assertNotNull(result);
        assertEquals(expectedPrice, result, 0.0001);
    }
    
    @Test
    void testGetCryptoPrice_NotInCache() {
        // Given - Pas de cache initialisé
        CryptoSymbol symbol = CryptoSymbol.BTC;
        
        // When
        Double result = cryptoPriceService.getCryptoPrice(symbol);
        
        // Then - Devrait être null car pas de cache
        assertNull(result);
    }
    
    @Test
    void testGetAllPrices() throws InterruptedException {
        // Given - Initialize cache first
        cryptoPriceService.initializePrices();
        // Wait for async operations
        Thread.sleep(200);
        
        // When
        Map<CryptoSymbol, Double> prices = cryptoPriceService.getAllPrices();
        
        // Then
        assertNotNull(prices);
        assertTrue(prices.containsKey(CryptoSymbol.BTC));
        assertTrue(prices.containsKey(CryptoSymbol.ETH));
        assertTrue(prices.containsKey(CryptoSymbol.SOL));
        assertEquals(50000.0, prices.get(CryptoSymbol.BTC), 0.0001);
        assertEquals(3000.0, prices.get(CryptoSymbol.ETH), 0.0001);
        assertEquals(150.0, prices.get(CryptoSymbol.SOL), 0.0001);
    }
    
    @Test
    void testRefreshPrices() throws InterruptedException {
        // Given - Réécrire le mock pour ce test
        Map<CryptoSymbol, Double> newPrices = Map.of(
                CryptoSymbol.BTC, 51000.0,
                CryptoSymbol.ETH, 3100.0,
                CryptoSymbol.SOL, 160.0
        );
        reset(coinGeckoClient);
        when(coinGeckoClient.getAllCryptoPricesInEur())
                .thenReturn(Mono.just(newPrices));
        
        // When
        cryptoPriceService.refreshPrices();
        // Wait for async operations
        Thread.sleep(200);
        
        // Then
        verify(coinGeckoClient, atLeastOnce()).getAllCryptoPricesInEur();
    }
    
    @Test
    void testGetCryptoPrice_ErrorHandling() throws InterruptedException {
        // Given - Réécrire le mock pour ce test
        reset(coinGeckoClient);
        when(coinGeckoClient.getAllCryptoPricesInEur())
                .thenReturn(Mono.empty());
        
        // When - Initialize avec erreur
        cryptoPriceService.initializePrices();
        // Wait for async operations
        Thread.sleep(200);
        
        // Then - Cache devrait être vide
        Double result = cryptoPriceService.getCryptoPrice(CryptoSymbol.BTC);
        assertNull(result);
    }
}

