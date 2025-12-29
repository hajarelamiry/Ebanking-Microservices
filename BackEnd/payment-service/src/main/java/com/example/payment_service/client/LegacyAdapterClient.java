package com.example.payment_service.client;

import com.example.payment_service.dto.LegacyPaymentRequest;
import com.example.payment_service.dto.LegacyPaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Client Feign pour communiquer avec le legacy-adapter-service (SOAP)
 * 
 * En mode MOCK, cette interface est désactivée et remplacée par MockLegacyAdapterClient
 */
@FeignClient(
    name = "legacy-adapter-service",
    url = "${feign.client.legacy-adapter-service.url:http://localhost:8082}",
    path = "/api/legacy/payments"
)
public interface LegacyAdapterClient {

    /**
     * Envoie un virement au système legacy (SOAP)
     */
    @PostMapping("/transfer")
    LegacyPaymentResponse sendPayment(@RequestBody LegacyPaymentRequest request);
}

