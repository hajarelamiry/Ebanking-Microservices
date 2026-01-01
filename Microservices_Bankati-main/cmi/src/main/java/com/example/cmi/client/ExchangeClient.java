package com.example.cmi.client;

import org.example.enums.Devise;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@FeignClient(name = "service-paiement-multidevises", url = "http://localhost:8083/api/exchange")
public interface ExchangeClient {
    @GetMapping("/rate")
    Double getExchangeRate(
            @RequestParam("fromCurrency") Devise fromCurrency,
            @RequestParam("toCurrency") Devise toCurrency
    );
}
