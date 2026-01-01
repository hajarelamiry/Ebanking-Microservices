package com.example.demo.service;

import com.example.demo.client.UserClient;
import com.example.demo.client.dto.UserInfoResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service pour récupérer les informations utilisateur depuis user-service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserClient userClient;
    
    /**
     * Récupère l'ID utilisateur depuis user-service via le username du JWT
     */
    public Long getUserIdFromUserService() {
        try {
            log.info("Attempting to retrieve user ID from user-service via Feign client");
            UserInfoResponse userInfo = userClient.getMyProfile();
            log.debug("Raw response from user-service: {}", userInfo);
            
            if (userInfo != null && userInfo.getId() != null) {
                log.info("Successfully retrieved user ID {} from user-service", userInfo.getId());
                return userInfo.getId();
            } else {
                log.warn("User info from user-service is null or has no ID. Response: {}", userInfo);
            }
        } catch (FeignException.Unauthorized e) {
            log.error("Authentication failed (401) - user-service returned Unauthorized. " +
                    "Check if JWT token is properly propagated via Feign. Status: {}, Message: {}", 
                    e.status(), e.getMessage());
            log.error("Response body: {}", e.contentUTF8());
        } catch (FeignException.Forbidden e) {
            log.error("Forbidden (403) - user-service returned Forbidden. Status: {}, Message: {}", 
                    e.status(), e.getMessage());
            log.error("Response body: {}", e.contentUTF8());
        } catch (FeignException.NotFound e) {
            log.error("Not Found (404) - user-service endpoint not found. Status: {}, Message: {}", 
                    e.status(), e.getMessage());
            log.error("Response body: {}", e.contentUTF8());
        } catch (FeignException e) {
            log.error("Feign error when retrieving user ID from user-service: Status={}, Message={}", 
                    e.status(), e.getMessage());
            if (e.contentUTF8() != null) {
                log.error("Response body: {}", e.contentUTF8());
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("HTTP client error when retrieving user ID from user-service: Status={}, Message={}", 
                    e.getStatusCode(), e.getMessage());
            if (e.getStatusCode().value() == 401) {
                log.error("Authentication failed - user-service returned 401. Check if JWT token is properly propagated.");
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Connection error when accessing user-service: {}", e.getMessage(), e);
            log.error("This usually means user-service is not accessible or Eureka cannot resolve the service name");
        } catch (Exception e) {
            log.error("Unexpected error when retrieving user ID from user-service: {}", e.getMessage(), e);
            log.error("Exception type: {}", e.getClass().getName());
        }
        return null;
    }
}
