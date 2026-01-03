package com.ebanking.user.controller;

import com.ebanking.user.entity.Customer;
import com.ebanking.user.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final CustomerRepository customerRepository;

    @GetMapping("/me")
    public Customer getMe(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return customerRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .keycloakId(keycloakId)
                            .email(jwt.getClaimAsString("email"))
                            .firstName(jwt.getClaimAsString("given_name"))
                            .lastName(jwt.getClaimAsString("family_name"))
                            .kycStatus(Customer.KycStatus.PENDING)
                            .build();
                    return customerRepository.save(newCustomer);
                });
    }

    @PutMapping("/profile")
    public Customer updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody Customer customerUpdates) {
        String keycloakId = jwt.getSubject();
        Customer customer = customerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setFirstName(customerUpdates.getFirstName());
        customer.setLastName(customerUpdates.getLastName());
        customer.setPhoneNumber(customerUpdates.getPhoneNumber());
        customer.setAddress(customerUpdates.getAddress());

        return customerRepository.save(customer);
    }

    @PostMapping("/kyc/submit")
    public Customer submitKyc(@AuthenticationPrincipal Jwt jwt, @RequestBody String documentUrl) {
        String keycloakId = jwt.getSubject();
        Customer customer = customerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setKycStatus(Customer.KycStatus.SUBMITTED);
        customer.setKycDocumentUrl(documentUrl);
        return customerRepository.save(customer);
    }

    @PutMapping("/kyc/validate/{id}")
    public Customer validateKyc(@PathVariable String id) {
        Customer customer = customerRepository.findByKeycloakId(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setKycStatus(Customer.KycStatus.VALIDATED);
        return customerRepository.save(customer);
    }

    @GetMapping
    public java.util.List<Customer> getAllUsers() {
        return customerRepository.findAll();
    }
}
