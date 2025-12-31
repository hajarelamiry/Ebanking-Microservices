package com.example.demo.service;

import com.example.demo.clients.CoinGeckoClient;
import com.example.demo.enums.CryptoSymbol;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CryptoPriceService {
    
    private final CoinGeckoClient coinGeckoClient;
    private final Map<CryptoSymbol, Double> priceCache = new ConcurrentHashMap<>();
    
    public CryptoPriceService(CoinGeckoClient coinGeckoClient) {
        this.coinGeckoClient = coinGeckoClient;
    }
    
    @PostConstruct
    public void initializePrices() {
        log.info("Initializing crypto prices with single API call...");
        fetchAllPricesAndCache();
    }
    
    public Double getCryptoPrice(CryptoSymbol symbol) {
        Double price = priceCache.get(symbol);
        if (price == null) {
            log.warn("Price not found in cache for {}, will be updated on next refresh", symbol);
        }
        return price;
    }
    
    @Scheduled(fixedRate = 12000) // Toutes les 12 secondes (10-15s recommandé)
    public void refreshPrices() {
        log.debug("Refreshing all crypto prices with single API call...");
        fetchAllPricesAndCache();
    }
    
    /**
     * Fait UN SEUL appel API pour récupérer TOUTES les cryptos
     */
    private void fetchAllPricesAndCache() {
        coinGeckoClient.getAllCryptoPricesInEur()
                .subscribe(
                        prices -> {
                            // Mettre à jour le cache avec tous les prix
                            prices.forEach((symbol, price) -> {
                                priceCache.put(symbol, price);
                                log.debug("✅ Updated price for {}: {} EUR", symbol, price);
                            });
                            log.info("✅ Successfully updated {} crypto prices", prices.size());
                        },
                        error -> {
                            // En cas d'erreur, on garde les anciens prix en cache
                            if (!priceCache.isEmpty()) {
                                log.debug("Keeping cached prices due to error: {}", error.getMessage());
                            } else {
                                log.warn("No cached prices available and API call failed: {}", error.getMessage());
                            }
                        }
                );
    }
    
    public Map<CryptoSymbol, Double> getAllPrices() {
        return Map.copyOf(priceCache);
    }
}

