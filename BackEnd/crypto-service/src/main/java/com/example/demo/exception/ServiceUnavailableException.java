package com.example.demo.exception;

/**
 * Exception levée quand un service externe est indisponible
 */
public class ServiceUnavailableException extends BusinessException {
    
    private final String serviceName;
    
    public ServiceUnavailableException(String serviceName, String message) {
        super("SERVICE_UNAVAILABLE", message);
        this.serviceName = serviceName;
    }
    
    public ServiceUnavailableException(String serviceName, String message, Throwable cause) {
        super("SERVICE_UNAVAILABLE", message, cause);
        this.serviceName = serviceName;
    }
    
    public String getServiceName() {
        return serviceName;
    }
}
