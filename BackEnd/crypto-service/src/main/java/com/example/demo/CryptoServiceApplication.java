package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Application principale du microservice Crypto Service
 * Eureka Client fonctionne automatiquement si la dépendance est présente
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling // Nécessaire pour CryptoPriceService.refreshPrices()
@EnableFeignClients
public class CryptoServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoServiceApplication.class, args);
	}

}
