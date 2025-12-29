package com.example.payment_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuration pour activer le mode Mock
 * Les services réels seront remplacés par des mocks
 */
@Configuration
@ConditionalOnProperty(name = "payment.mock.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class MockConfiguration {

    @PostConstruct
    public void init() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("🔵 MODE MOCK ACTIVÉ - Services externes simulés");
        log.info("🔵 AccountService: MOCK (MockAccountService)");
        log.info("🔵 LegacyBankingService: MOCK (MockLegacyBankingService)");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("💡 Pour désactiver les mocks et utiliser les vrais services:");
        log.info("   - payment.mock.enabled=false");
        log.info("   - Les implémentations Feign seront utilisées");
        log.info("═══════════════════════════════════════════════════════════");
    }
}

