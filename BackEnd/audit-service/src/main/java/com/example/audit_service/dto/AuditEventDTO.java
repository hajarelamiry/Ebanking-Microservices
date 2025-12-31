package com.example.audit_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AuditEventDTO {

    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "Action type is required")
    private String actionType;

    @NotBlank(message = "Service name is required")
    private String serviceName;

    private String description;

    private String details;

    @NotBlank(message = "Status is required")
    private String status; // SUCCESS, FAILURE, ERROR

    private String errorMessage;

    private String ipAddress;

    private String userAgent;

    private String correlationId; // ID de corrélation pour tracer la transaction

    // Constructors
    public AuditEventDTO() {
    }

    public AuditEventDTO(String userId, String actionType, String serviceName, String description, String status) {
        this.userId = userId;
        this.actionType = actionType;
        this.serviceName = serviceName;
        this.description = description;
        this.status = status;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
}

