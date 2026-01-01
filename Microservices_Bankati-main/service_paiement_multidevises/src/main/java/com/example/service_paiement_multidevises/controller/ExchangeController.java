package com.example.service_paiement_multidevises.controller;

import com.example.service_paiement_multidevises.service.ExchangeRateService;
import org.example.enums.Devise;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange")
public class ExchangeController {


    @Autowired
    private ExchangeRateService exchangeService;
    @GetMapping("/rate")
    public Double getExchangeRate(
            @RequestParam("fromCurrency") Devise fromCurrency,
            @RequestParam("toCurrency") Devise toCurrency) {
        return exchangeService.getExchangeRate(fromCurrency, toCurrency);
    }
}
