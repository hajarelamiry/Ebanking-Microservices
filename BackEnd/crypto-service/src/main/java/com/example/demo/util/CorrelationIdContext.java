package com.example.demo.util;

import org.slf4j.MDC;

/**
 * Utilitaire pour accéder au Correlation ID depuis n'importe où dans l'application
 */
public class CorrelationIdContext {

    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    /**
     * Récupère le Correlation ID actuel depuis le MDC
     * @return Le Correlation ID ou null si non défini
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_MDC_KEY);
    }

    /**
     * Définit le Correlation ID dans le MDC
     * @param correlationId Le Correlation ID à définir
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null && !correlationId.trim().isEmpty()) {
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);
        }
    }

    /**
     * Supprime le Correlation ID du MDC
     */
    public static void clear() {
        MDC.remove(CORRELATION_ID_MDC_KEY);
    }
}

