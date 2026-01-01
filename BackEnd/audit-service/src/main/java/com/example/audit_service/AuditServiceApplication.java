package com.example.audit_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Application principale du microservice Audit Service
 * Eureka Client fonctionne automatiquement si la dépendance est présente
 */
@EnableDiscoveryClient
@SpringBootApplication
public class AuditServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuditServiceApplication.class, args);
	}

}
