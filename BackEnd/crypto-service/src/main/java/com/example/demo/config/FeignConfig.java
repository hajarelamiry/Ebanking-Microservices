package com.example.demo.config;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Configuration Feign pour propager le token JWT entre microservices
 */
@Configuration
@Slf4j
public class FeignConfig {

    /**
     * Intercepteur pour propager le token JWT dans les requêtes Feign
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            log.debug("Feign RequestInterceptor - Authentication: {}", authentication != null ? "present" : "null");
            
            if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
                Jwt jwt = (Jwt) authentication.getPrincipal();
                String token = jwt.getTokenValue();
                String username = jwt.getClaimAsString("preferred_username");
                log.debug("Feign RequestInterceptor - Adding JWT token for user: {}, token length: {}", 
                        username, token != null ? token.length() : 0);
                requestTemplate.header("Authorization", "Bearer " + token);
            } else {
                log.warn("Feign RequestInterceptor - No JWT token available in SecurityContext. " +
                        "Authentication: {}, Principal type: {}", 
                        authentication != null, 
                        authentication != null && authentication.getPrincipal() != null ? 
                                authentication.getPrincipal().getClass().getName() : "null");
            }
        };
    }
}
