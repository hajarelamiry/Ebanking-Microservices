package com.ebanking.user.controller;

import com.ebanking.user.entity.Customer;
import com.ebanking.user.entity.UserCreationRequest;
import com.ebanking.user.repository.CustomerRepository;
import com.ebanking.user.repository.UserCreationRequestRepository;
import com.ebanking.user.service.EmailService;
import com.ebanking.user.service.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class CustomerGraphQLController {

    private final CustomerRepository customerRepository;
    private final UserCreationRequestRepository requestRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final EmailService emailService;

    @QueryMapping
    public Customer me(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            System.err.println("Me Query Error: JWT is null (User not authenticated)");
            throw new RuntimeException("Unauthorized");
        }
        String keycloakId = jwt.getSubject();
        System.out.println("Me Query for KeycloakId: " + keycloakId);

        return customerRepository.findByKeycloakId(keycloakId)
                .map(c -> {
                    System.out.println("Me Query Success: Customer found - " + c.getEmail());
                    return c;
                })
                .orElseThrow(() -> {
                    System.err.println("Me Query Error: Customer not found in DB for ID: " + keycloakId);
                    return new RuntimeException("Customer not found");
                });
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public List<Customer> users() {
        System.out.println("--- Users Query Started in User-Service ---");
        try {
            List<Customer> users = customerRepository.findAll();
            System.out.println("--- Users Query Success in User-Service (count: " + users.size() + ") ---");
            return users;
        } catch (Exception e) {
            System.err.println("--- Users Query Failed in User-Service ---");
            e.printStackTrace();
            throw e;
        }
    }

    @MutationMapping
    public Customer updateProfile(@AuthenticationPrincipal Jwt jwt, @Argument Map<String, Object> input) {
        String keycloakId = jwt.getSubject();
        Customer customer = customerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        if (input.containsKey("firstName"))
            customer.setFirstName((String) input.get("firstName"));
        if (input.containsKey("lastName"))
            customer.setLastName((String) input.get("lastName"));
        if (input.containsKey("phoneNumber"))
            customer.setPhoneNumber((String) input.get("phoneNumber"));
        if (input.containsKey("address"))
            customer.setAddress((String) input.get("address"));

        return customerRepository.save(customer);
    }

    @MutationMapping
    public Customer submitKyc(@AuthenticationPrincipal Jwt jwt, @Argument String documentUrl) {
        System.out.println("--- SubmitKyc Started ---");
        String keycloakId = jwt.getSubject();
        System.out.println("User KeycloakId: " + keycloakId);

        Customer customer = customerRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> {
                    System.err.println("SubmitKyc Error: Customer not found for ID " + keycloakId);
                    return new RuntimeException("Customer not found");
                });
        System.out.println("Customer found: " + customer.getEmail());

        customer.setKycStatus(Customer.KycStatus.SUBMITTED);
        customer.setKycDocumentUrl(documentUrl);

        System.out.println("Saving customer with SUBMITTED status...");
        Customer saved = customerRepository.save(customer);
        System.out.println("--- SubmitKyc Success ---");
        return saved;
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public Customer validateKyc(@Argument String id) {
        Customer customer = customerRepository.findByKeycloakId(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        customer.setKycStatus(Customer.KycStatus.VALIDATED);
        return customerRepository.save(customer);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public Customer createCustomer(@Argument Map<String, Object> input) {
        Customer newCustomer = Customer.builder()
                .keycloakId((String) input.get("keycloakId"))
                .firstName((String) input.get("firstName"))
                .lastName((String) input.get("lastName"))
                .email((String) input.get("email"))
                .kycStatus(Customer.KycStatus.PENDING)
                .build();
        return customerRepository.save(newCustomer);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public boolean deleteUser(@Argument String id) {
        Customer customer = customerRepository.findByKeycloakId(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        customerRepository.delete(customer);
        return true;
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public List<UserCreationRequest> pendingRequests() {
        System.out.println("--- PendingRequests Query Started in User-Service ---");
        try {
            List<UserCreationRequest> requests = requestRepository
                    .findByStatus(UserCreationRequest.RequestStatus.PENDING);
            System.out
                    .println("--- PendingRequests Query Success in User-Service (count: " + requests.size() + ") ---");
            return requests;
        } catch (Exception e) {
            System.err.println("--- PendingRequests Query Failed in User-Service ---");
            e.printStackTrace();
            throw e;
        }
    }

    @MutationMapping
    @PreAuthorize("hasRole('AGENT')")
    public UserCreationRequest requestUserCreation(@Argument Map<String, Object> input,
            @AuthenticationPrincipal Jwt jwt) {
        System.out.println("--- RequestUserCreation Mutation Started in User-Service ---");
        System.out.println("Input: " + input);
        try {
            UserCreationRequest request = UserCreationRequest.builder()
                    .firstName((String) input.get("firstName"))
                    .lastName((String) input.get("lastName"))
                    .email((String) input.get("email"))
                    .status(UserCreationRequest.RequestStatus.PENDING)
                    .agentId(jwt.getSubject())
                    .createdAt(LocalDateTime.now())
                    .build();
            UserCreationRequest savedRequest = requestRepository.save(request);
            System.out.println("--- RequestUserCreation Mutation Success in User-Service ---");
            return savedRequest;
        } catch (Exception e) {
            System.err.println("--- RequestUserCreation Mutation Failed in User-Service ---");
            e.printStackTrace();
            throw e;
        }
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserCreationRequest processUserCreation(@Argument Long id, @Argument String status,
            @Argument String reason) {
        UserCreationRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        UserCreationRequest.RequestStatus newStatus = UserCreationRequest.RequestStatus.valueOf(status);
        request.setStatus(newStatus);
        request.setProcessedAt(LocalDateTime.now());

        if (newStatus == UserCreationRequest.RequestStatus.APPROVED) {
            // 1. Generate Temp Password
            String tempPassword = "Cap" + (int) (Math.random() * 9000 + 1000) + "!";

            // 2. Create in Keycloak
            String keycloakId = keycloakAdminService.createUser(
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    tempPassword);

            // 3. Create Local Customer Profile
            // Check if customer already exists (idempotency)
            Customer customer = customerRepository.findByKeycloakId(keycloakId)
                    .orElseGet(() -> {
                        Customer newCustomer = Customer.builder()
                                .keycloakId(keycloakId)
                                .firstName(request.getFirstName())
                                .lastName(request.getLastName())
                                .email(request.getEmail())
                                .kycStatus(Customer.KycStatus.PENDING)
                                .build();
                        return customerRepository.save(newCustomer);
                    });

            // 4. Send Email
            emailService.sendCredentials(request.getEmail(), request.getFirstName(), request.getEmail(), tempPassword);
        }

        return requestRepository.save(request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Customer onboardUser(@Argument Map<String, Object> input) {
        System.out.println("--- OnboardUser Mutation Started in User-Service ---");
        System.out.println("Input: " + input);
        try {
            String email = (String) input.get("email");
            String firstName = (String) input.get("firstName");
            String lastName = (String) input.get("lastName");

            if (email == null || firstName == null || lastName == null) {
                System.err.println("Error: Missing required fields in input");
                throw new RuntimeException("Missing required fields: email, firstName, or lastName");
            }

            // 1. Generate Temp Password
            String tempPassword = "Cap" + (int) (Math.random() * 9000 + 1000) + "!";

            // 2. Create in Keycloak
            String keycloakId = keycloakAdminService.createUser(email, firstName, lastName, tempPassword);

            // 3. Create Local Customer Profile
            Customer customer = Customer.builder()
                    .keycloakId(keycloakId)
                    .firstName(firstName)
                    .lastName(lastName)
                    .email(email)
                    .kycStatus(Customer.KycStatus.PENDING)
                    .build();
            customerRepository.save(customer);

            // 4. Send Email
            emailService.sendCredentials(email, firstName, email, tempPassword);

            System.out.println("--- OnboardUser Mutation Success in User-Service ---");
            return customer;
        } catch (Exception e) {
            System.err.println("--- OnboardUser Mutation Failed in User-Service ---");
            e.printStackTrace();
            throw e;
        }
    }
}
