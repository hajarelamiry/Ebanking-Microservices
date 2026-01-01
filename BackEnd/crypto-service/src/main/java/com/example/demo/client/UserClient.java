package com.example.demo.client;

import com.example.demo.client.dto.UserInfoResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Feign Client pour communiquer avec User Service via Eureka
 */
@FeignClient(name = "user-service")
public interface UserClient {
    
    /**
     * Récupère les informations de l'utilisateur connecté
     * Le token JWT sera automatiquement propagé via FeignConfig
     */
    @GetMapping("/api/customers/me")
    UserInfoResponse getMyProfile();
}
