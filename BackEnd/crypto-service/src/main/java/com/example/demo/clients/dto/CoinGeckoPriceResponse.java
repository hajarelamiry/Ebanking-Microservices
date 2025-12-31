package com.example.demo.clients.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class CoinGeckoPriceResponse {
    
    private Map<String, Map<String, Double>> prices = new HashMap<>();
    
    @JsonAnySetter
    public void setPrice(String coinId, Map<String, Double> priceData) {
        prices.put(coinId, priceData);
    }
    
    public Map<String, Double> getPriceForCoin(String coinId) {
        return prices.get(coinId);
    }
    
    public void setPriceForCoin(String coinId, Map<String, Double> priceData) {
        prices.put(coinId, priceData);
    }
}

