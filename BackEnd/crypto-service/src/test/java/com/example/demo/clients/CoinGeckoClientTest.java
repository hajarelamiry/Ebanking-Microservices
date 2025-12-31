package com.example.demo.clients;

import com.example.demo.clients.dto.CoinGeckoPriceResponse;
import com.example.demo.enums.CryptoSymbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoinGeckoClientTest {
    
    @Mock
    private WebClient webClient;
    
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    
    @Mock
    private WebClient.ResponseSpec responseSpec;
    
    private CoinGeckoClient coinGeckoClient;
    
    @BeforeEach
    void setUp() {
        coinGeckoClient = new CoinGeckoClient(
                org.springframework.web.reactive.function.client.WebClient.builder(),
                "https://api.coingecko.com/api/v3"
        );
        ReflectionTestUtils.setField(coinGeckoClient, "webClient", webClient);
    }
    
    @Test
    void testGetCryptoPriceInEur_Bitcoin_Success() {
        // Given
        CryptoSymbol symbol = CryptoSymbol.BTC;
        Double expectedPrice = 50000.0;
        
        CoinGeckoPriceResponse response = new CoinGeckoPriceResponse();
        Map<String, Double> priceData = new HashMap<>();
        priceData.put("eur", expectedPrice);
        response.setPriceForCoin("bitcoin", priceData);
        
        // Mock uniquement ce qui est utilisé
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CoinGeckoPriceResponse.class))
                .thenReturn(Mono.just(response));
        
        // When
        Mono<Double> result = coinGeckoClient.getCryptoPriceInEur(symbol);
        
        // Then
        StepVerifier.create(result)
                .expectNext(expectedPrice)
                .verifyComplete();
    }
    
    @Test
    void testGetCryptoPriceInEur_Ethereum_Success() {
        // Given
        CryptoSymbol symbol = CryptoSymbol.ETH;
        Double expectedPrice = 3000.0;
        
        CoinGeckoPriceResponse response = new CoinGeckoPriceResponse();
        Map<String, Double> priceData = new HashMap<>();
        priceData.put("eur", expectedPrice);
        response.setPriceForCoin("ethereum", priceData);
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CoinGeckoPriceResponse.class))
                .thenReturn(Mono.just(response));
        
        // When
        Mono<Double> result = coinGeckoClient.getCryptoPriceInEur(symbol);
        
        // Then
        StepVerifier.create(result)
                .expectNext(expectedPrice)
                .verifyComplete();
    }
    
    @Test
    void testGetCryptoPriceInEur_Solana_Success() {
        // Given
        CryptoSymbol symbol = CryptoSymbol.SOL;
        Double expectedPrice = 150.0;
        
        CoinGeckoPriceResponse response = new CoinGeckoPriceResponse();
        Map<String, Double> priceData = new HashMap<>();
        priceData.put("eur", expectedPrice);
        response.setPriceForCoin("solana", priceData);
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CoinGeckoPriceResponse.class))
                .thenReturn(Mono.just(response));
        
        // When
        Mono<Double> result = coinGeckoClient.getCryptoPriceInEur(symbol);
        
        // Then
        StepVerifier.create(result)
                .expectNext(expectedPrice)
                .verifyComplete();
    }
    
    @Test
    void testGetCryptoPriceInEur_ErrorHandling_ReturnsEmpty() {
        // Given
        CryptoSymbol symbol = CryptoSymbol.BTC;
        
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CoinGeckoPriceResponse.class))
                .thenReturn(Mono.error(WebClientResponseException.create(500, "Server Error", null, null, null)));
        
        // When
        Mono<Double> result = coinGeckoClient.getCryptoPriceInEur(symbol);
        
        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }
    
    @Test
    void testGetAllCryptoPricesInEur_Success() {
        // Given
        CoinGeckoPriceResponse response = new CoinGeckoPriceResponse();
        
        Map<String, Double> bitcoinData = new HashMap<>();
        bitcoinData.put("eur", 50000.0);
        response.setPriceForCoin("bitcoin", bitcoinData);
        
        Map<String, Double> ethereumData = new HashMap<>();
        ethereumData.put("eur", 3000.0);
        response.setPriceForCoin("ethereum", ethereumData);
        
        Map<String, Double> solanaData = new HashMap<>();
        solanaData.put("eur", 150.0);
        response.setPriceForCoin("solana", solanaData);
        
        // Mock uniquement ce qui est utilisé
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CoinGeckoPriceResponse.class))
                .thenReturn(Mono.just(response));
        
        // When
        Mono<Map<CryptoSymbol, Double>> result = coinGeckoClient.getAllCryptoPricesInEur();
        
        // Then
        StepVerifier.create(result)
                .assertNext(prices -> {
                    assertEquals(3, prices.size());
                    assertEquals(50000.0, prices.get(CryptoSymbol.BTC), 0.0001);
                    assertEquals(3000.0, prices.get(CryptoSymbol.ETH), 0.0001);
                    assertEquals(150.0, prices.get(CryptoSymbol.SOL), 0.0001);
                })
                .verifyComplete();
    }
    
    @Test
    void testGetAllCryptoPricesInEur_ErrorHandling_ReturnsEmpty() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(CoinGeckoPriceResponse.class))
                .thenReturn(Mono.error(WebClientResponseException.create(429, "Rate Limit", null, null, null)));
        
        // When
        Mono<Map<CryptoSymbol, Double>> result = coinGeckoClient.getAllCryptoPricesInEur();
        
        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }
}

