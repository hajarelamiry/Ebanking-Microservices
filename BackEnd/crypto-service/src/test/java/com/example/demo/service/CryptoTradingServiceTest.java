package com.example.demo.service;

import com.example.demo.enums.CryptoSymbol;
import com.example.demo.enums.TradeType;
import com.example.demo.model.CryptoTransaction;
import com.example.demo.model.CryptoWallet;
import com.example.demo.repository.CryptoTransactionRepository;
import com.example.demo.repository.CryptoWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CryptoTradingServiceTest {
    
    @Mock
    private CryptoWalletRepository walletRepository;
    
    @Mock
    private CryptoTransactionRepository transactionRepository;
    
    @Mock
    private CryptoPriceService priceService;
    
    @InjectMocks
    private CryptoTradingService tradingService;
    
    private static final Long USER_ID = 1L;
    private static final CryptoSymbol SYMBOL = CryptoSymbol.BTC;
    private static final Double PRICE = 50000.0;
    
    @BeforeEach
    void setUp() {
        when(priceService.getCryptoPrice(any(CryptoSymbol.class))).thenReturn(PRICE);
    }
    
    @Test
    void testTrade_Buy_NewWallet() {
        // Given
        Double quantity = 0.5;
        TradeType type = TradeType.BUY;
        
        when(walletRepository.findByUserIdAndSymbol(USER_ID, SYMBOL))
                .thenReturn(Optional.empty());
        when(walletRepository.save(any(CryptoWallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(CryptoTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CryptoTransaction transaction = tradingService.trade(USER_ID, SYMBOL, quantity, type);
        
        // Then
        assertNotNull(transaction);
        assertEquals(USER_ID, transaction.getUserId());
        assertEquals(SYMBOL, transaction.getSymbol());
        assertEquals(type, transaction.getType());
        assertEquals(quantity, transaction.getQuantity(), 0.0001);
        assertEquals(PRICE, transaction.getPriceAtTime(), 0.0001);
        
        verify(walletRepository).save(argThat(wallet ->
                wallet.getUserId().equals(USER_ID) &&
                wallet.getSymbol().equals(SYMBOL) &&
                wallet.getBalance().equals(quantity)
        ));
        verify(transactionRepository).save(any(CryptoTransaction.class));
    }
    
    @Test
    void testTrade_Buy_ExistingWallet() {
        // Given
        Double existingBalance = 1.0;
        Double quantity = 0.5;
        TradeType type = TradeType.BUY;
        
        CryptoWallet existingWallet = CryptoWallet.builder()
                .id(1L)
                .userId(USER_ID)
                .symbol(SYMBOL)
                .balance(existingBalance)
                .build();
        
        when(walletRepository.findByUserIdAndSymbol(USER_ID, SYMBOL))
                .thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(CryptoWallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(CryptoTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CryptoTransaction transaction = tradingService.trade(USER_ID, SYMBOL, quantity, type);
        
        // Then
        assertNotNull(transaction);
        verify(walletRepository).save(argThat(wallet ->
                wallet.getBalance().equals(existingBalance + quantity)
        ));
    }
    
    @Test
    void testTrade_Sell_SufficientBalance() {
        // Given
        Double existingBalance = 2.0;
        Double quantity = 0.5;
        TradeType type = TradeType.SELL;
        
        CryptoWallet existingWallet = CryptoWallet.builder()
                .id(1L)
                .userId(USER_ID)
                .symbol(SYMBOL)
                .balance(existingBalance)
                .build();
        
        when(walletRepository.findByUserIdAndSymbol(USER_ID, SYMBOL))
                .thenReturn(Optional.of(existingWallet));
        when(walletRepository.save(any(CryptoWallet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(CryptoTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        CryptoTransaction transaction = tradingService.trade(USER_ID, SYMBOL, quantity, type);
        
        // Then
        assertNotNull(transaction);
        assertEquals(TradeType.SELL, transaction.getType());
        verify(walletRepository).save(argThat(wallet ->
                wallet.getBalance().equals(existingBalance - quantity)
        ));
    }
    
    @Test
    void testTrade_Sell_InsufficientBalance() {
        // Given
        Double existingBalance = 0.3;
        Double quantity = 0.5;
        TradeType type = TradeType.SELL;
        
        CryptoWallet existingWallet = CryptoWallet.builder()
                .id(1L)
                .userId(USER_ID)
                .symbol(SYMBOL)
                .balance(existingBalance)
                .build();
        
        when(walletRepository.findByUserIdAndSymbol(USER_ID, SYMBOL))
                .thenReturn(Optional.of(existingWallet));
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tradingService.trade(USER_ID, SYMBOL, quantity, type)
        );
        
        assertTrue(exception.getMessage().contains("Insufficient balance"));
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
    
    @Test
    void testTrade_InvalidQuantity_Zero() {
        // Given
        Double quantity = 0.0;
        TradeType type = TradeType.BUY;
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tradingService.trade(USER_ID, SYMBOL, quantity, type)
        );
        
        assertEquals("Quantity must be greater than 0", exception.getMessage());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
    
    @Test
    void testTrade_InvalidQuantity_Negative() {
        // Given
        Double quantity = -1.0;
        TradeType type = TradeType.BUY;
        
        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tradingService.trade(USER_ID, SYMBOL, quantity, type)
        );
        
        assertEquals("Quantity must be greater than 0", exception.getMessage());
    }
    
    @Test
    void testTrade_PriceNotAvailable() {
        // Given
        Double quantity = 0.5;
        TradeType type = TradeType.BUY;
        
        when(priceService.getCryptoPrice(SYMBOL)).thenReturn(null);
        
        // When & Then
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> tradingService.trade(USER_ID, SYMBOL, quantity, type)
        );
        
        assertTrue(exception.getMessage().contains("Price not available"));
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}

