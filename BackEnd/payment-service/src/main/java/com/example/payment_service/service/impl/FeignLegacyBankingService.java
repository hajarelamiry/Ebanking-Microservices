package com.example.payment_service.service.impl;

import com.example.payment_service.client.LegacyAdapterClient;
import com.example.payment_service.dto.LegacyPaymentRequest;
import com.example.payment_service.dto.LegacyPaymentResponse;
import com.example.payment_service.service.LegacyBankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Implémentation réelle utilisant Feign pour communiquer avec legacy-adapter-service
 * Activée uniquement si payment.mock.enabled=false
 */
@Service
@ConditionalOnProperty(name = "payment.mock.enabled", havingValue = "false", matchIfMissing = false)
@RequiredArgsConstructor
@Slf4j
public class FeignLegacyBankingService implements LegacyBankingService {

    private final LegacyAdapterClient legacyAdapterClient;

    @Override
    public LegacyPaymentResponse sendPayment(LegacyPaymentRequest request) {
        log.debug("Appel réel à legacy-adapter-service pour la transaction: {}", request.getTransactionId());
        return legacyAdapterClient.sendPayment(request);
    }
}

