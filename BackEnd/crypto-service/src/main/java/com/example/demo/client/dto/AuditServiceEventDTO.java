package com.example.demo.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO compatible avec audit-service pour la communication via Feign
 * Ce DTO correspond exactement à com.example.audit_service.dto.AuditEventDTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditServiceEventDTO {
    private String userId;
    private String actionType;
    private String serviceName;
    private String description;
    private String details;
    private String status;
    private String errorMessage;
    private String ipAddress;
    private String userAgent;
    private String correlationId;
}

