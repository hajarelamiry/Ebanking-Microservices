package com.example.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filtre pour générer et propager le Correlation ID dans toutes les requêtes HTTP
 * Le Correlation ID permet de tracer une transaction à travers tous les microservices
 */
@Component
@Order(1)
@Slf4j
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Récupère le Correlation ID depuis le header ou en génère un nouveau
        String correlationId = getCorrelationId(request);
        
        // Ajoute le Correlation ID dans le MDC (Mapped Diagnostic Context) pour les logs
        MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        
        // Ajoute le Correlation ID dans la réponse HTTP
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Nettoie le MDC après la requête
            MDC.clear();
        }
    }

    /**
     * Récupère le Correlation ID depuis le header de la requête ou en génère un nouveau
     */
    private String getCorrelationId(HttpServletRequest request) {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            log.debug("Generated new correlation ID: {}", correlationId);
        } else {
            log.debug("Using existing correlation ID: {}", correlationId);
        }
        return correlationId;
    }
}

