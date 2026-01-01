package com.example.service_portefeuilles.service;

import org.example.enums.Devise;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class ExchangeRateService {

    @Value("${exchangerate.api.url}")
    private String apiUrl;

    @Value("${exchangerate.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    public ExchangeRateService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Double getExchangeRate(Devise fromCurrency, Devise toCurrency) {
        // Convertir les devises en chaînes de caractères compatibles avec l'API
        String fromCurrencyString = fromCurrency.name();
        String toCurrencyString = toCurrency.name();

        // Construire l'URL avec la devise de base
        String url = apiUrl + fromCurrencyString + "?apikey=" + apiKey;

        // Appeler l'API pour récupérer les taux
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);
        if (response == null || !response.containsKey("conversion_rates")) {
            throw new RuntimeException("Erreur lors de la récupération des taux de change");
        }

        // Extraire les taux de conversion
        Map<String, Object> rates = (Map<String, Object>) response.get("conversion_rates");

        // Vérifier que la devise cible existe dans les taux de conversion
        if (!rates.containsKey(toCurrencyString)) {
            throw new RuntimeException("Devise cible non trouvée dans les taux de conversion");
        }
        // Convertir la valeur de la devise cible en Double
        Object rate = rates.get(toCurrencyString);
        if (rate instanceof Integer) {
            return ((Integer) rate).doubleValue();
        } else if (rate instanceof Double) {
            return (Double) rate;
        } else {
            throw new RuntimeException("Type de taux de change non pris en charge : " + rate.getClass());
        }
    }

}
