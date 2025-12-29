package com.example.payment_service.config;

import feign.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration pour les clients Feign
 * Note: @EnableFeignClients est dans PaymentServiceApplication
 * 
 * En mode MOCK, les clients Feign réels sont désactivés
 */
@Configuration
public class FeignConfig {

    @Bean
    @ConditionalOnProperty(name = "payment.mock.enabled", havingValue = "false", matchIfMissing = false)
    @ConditionalOnMissingBean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}

