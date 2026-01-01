package com.ebanking.user.controller;

import com.ebanking.user.model.Customer;
import com.ebanking.user.service.CustomerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping("/me")
    public Customer getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        // Extraire le username depuis le JWT (preferred_username)
        String username = jwt.getClaimAsString("preferred_username");
        
        if (username == null || username.isEmpty()) {
            throw new RuntimeException("JWT token does not contain preferred_username");
        }
        
        // Essayer de récupérer le customer existant
        try {
            return customerService.getCustomerByUsername(username);
        } catch (RuntimeException e) {
            // Si le customer n'existe pas (message contient "not found"), le créer automatiquement
            if (e.getMessage() != null && e.getMessage().contains("not found")) {
                // Auto-create for demo simplicity if not exists
                Customer newCustomer = Customer.builder()
                        .username(username)
                        .email(jwt.getClaimAsString("email"))
                        .firstName(jwt.getClaimAsString("given_name"))
                        .lastName(jwt.getClaimAsString("family_name"))
                        .build();
                try {
                    return customerService.createCustomer(newCustomer);
                } catch (RuntimeException createException) {
                    // Si la création échoue (ex: customer existe déjà), réessayer de le récupérer
                    if (createException.getMessage() != null && createException.getMessage().contains("already exists")) {
                        return customerService.getCustomerByUsername(username);
                    }
                    throw createException;
                }
            }
            // Pour les autres erreurs, re-lancer l'exception
            throw e;
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public java.util.List<Customer> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
    }

    @PatchMapping("/{id}/kyc")
    @PreAuthorize("hasRole('AGENT')")
    public Customer updateKYC(@PathVariable Long id,
            @RequestBody com.ebanking.user.dto.CustomerDTOs.UpdateKYCRequest request) {
        return customerService.updateKYC(id, request.getStatus());
    }

    @PutMapping("/me")
    public Customer updateMyProfile(@AuthenticationPrincipal Jwt jwt,
            @RequestBody com.ebanking.user.dto.CustomerDTOs.UpdateProfileRequest request) {
        String username = jwt.getClaimAsString("preferred_username");
        return customerService.updateProfile(username, request);
    }
}
