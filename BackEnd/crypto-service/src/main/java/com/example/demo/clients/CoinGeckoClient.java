package com.example.demo.clients;

import com.example.demo.clients.dto.CoinGeckoPriceResponse;
import com.example.demo.enums.CryptoSymbol;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@Slf4j
public class CoinGeckoClient {
    
    private final WebClient webClient;
    
    public CoinGeckoClient(
            WebClient.Builder webClientBuilder,
            @Value("${coingecko.api.url:https://api.coingecko.com/api/v3}") String apiUrl) {
        this.webClient = webClientBuilder
                .baseUrl(apiUrl)
                .build();
    }
    
    /**
     * Récupère les prix de TOUTES les cryptos en UN SEUL appel API
     * @return Map des prix par symbole crypto
     */
    public Mono<Map<CryptoSymbol, Double>> getAllCryptoPricesInEur() {
        // Construire la liste des IDs séparés par des virgules
        String allCoinIds = "bitcoin,ethereum,solana";
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/simple/price")
                        .queryParam("ids", allCoinIds)
                        .queryParam("vs_currencies", "eur")
                        .build())
                .retrieve()
                .bodyToMono(CoinGeckoPriceResponse.class)
                .map(response -> {
                    Map<CryptoSymbol, Double> prices = new java.util.HashMap<>();
                    
                    // Extraire le prix pour chaque crypto
                    Map<String, Double> bitcoinData = response.getPriceForCoin("bitcoin");
                    if (bitcoinData != null && bitcoinData.containsKey("eur")) {
                        prices.put(CryptoSymbol.BTC, bitcoinData.get("eur"));
                    }
                    
                    Map<String, Double> ethereumData = response.getPriceForCoin("ethereum");
                    if (ethereumData != null && ethereumData.containsKey("eur")) {
                        prices.put(CryptoSymbol.ETH, ethereumData.get("eur"));
                    }
                    
                    Map<String, Double> solanaData = response.getPriceForCoin("solana");
                    if (solanaData != null && solanaData.containsKey("eur")) {
                        prices.put(CryptoSymbol.SOL, solanaData.get("eur"));
                    }
                    
                    return prices;
                })
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) error;
                        if (ex.getStatusCode().value() == 429) {
                            log.warn("⚠️ CoinGecko API rate limit (429) - Too many requests");
                        } else {
                            log.error("❌ CoinGecko API error {}: {}", ex.getStatusCode(), ex.getMessage());
                        }
                    } else if (error.getMessage() != null && error.getMessage().contains("resolve")) {
                        log.warn("⚠️ DNS error - Network issue, not code: {}", error.getMessage());
                    } else {
                        log.error("❌ Unexpected error fetching prices: {}", error.getMessage());
                    }
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 429) {
                        log.warn("Rate limit hit - skipping this refresh cycle");
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    // Erreur réseau/DNS - garder l'ancien prix en cache
                    return Mono.empty();
                });
    }
    
    @Deprecated
    public Mono<Double> getCryptoPriceInEur(CryptoSymbol symbol) {
        // Méthode conservée pour compatibilité mais ne devrait plus être utilisée
        String coinId = mapSymbolToCoinId(symbol);
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/simple/price")
                        .queryParam("ids", coinId)
                        .queryParam("vs_currencies", "eur")
                        .build())
                .retrieve()
                .bodyToMono(CoinGeckoPriceResponse.class)
                .map(response -> extractPrice(response, coinId))
                .doOnError(error -> {
                    if (error instanceof WebClientResponseException) {
                        WebClientResponseException ex = (WebClientResponseException) error;
                        if (ex.getStatusCode().value() == 429) {
                            log.warn("⚠️ CoinGecko API rate limit (429) for {} - Too many requests", symbol);
                        } else {
                            log.error("❌ CoinGecko API error {} for {}: {}", ex.getStatusCode(), symbol, ex.getMessage());
                        }
                    } else if (error.getMessage() != null && error.getMessage().contains("resolve")) {
                        log.warn("⚠️ DNS error for {} - Network issue, not code: {}", symbol, error.getMessage());
                    } else {
                        log.error("❌ Unexpected error fetching price for {}: {}", symbol, error.getMessage());
                    }
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    if (e.getStatusCode().value() == 429) {
                        log.warn("Rate limit hit - skipping this refresh cycle");
                    }
                    return Mono.empty();
                })
                .onErrorResume(e -> {
                    return Mono.empty();
                });
    }
    
    private String mapSymbolToCoinId(CryptoSymbol symbol) {
        return switch (symbol) {
            case BTC -> "bitcoin";
            case ETH -> "ethereum";
            case SOL -> "solana";
        };
    }
    
    private Double extractPrice(CoinGeckoPriceResponse response, String coinId) {
        Map<String, Double> coinData = response.getPriceForCoin(coinId);
        
        if (coinData != null && coinData.containsKey("eur")) {
            return coinData.get("eur");
        }
        
        throw new RuntimeException("Price not found for " + coinId);
    }
}

