package com.ebanking.user.service;

import com.ebanking.user.entity.Customer;
import com.ebanking.user.entity.Customer.KycStatus;
import com.ebanking.user.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer getCustomerByUsername(String username) {
        return customerRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + username));
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        if (customerRepository.findByUsername(customer.getUsername()).isPresent()) {
            throw new RuntimeException("Customer already exists");
        }
        customer.setKycStatus(KycStatus.PENDING);
        customer.setRgpdConsent(true); // Default consent for demo
        return customerRepository.save(customer);
    }

    public java.util.List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
    }

    @Transactional
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    @Transactional
    public Customer updateKYC(Long id, KycStatus status) {
        Customer customer = getCustomerById(id);
        customer.setKycStatus(status);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateProfile(String username, com.ebanking.user.dto.CustomerDTOs.UpdateProfileRequest request) {
        Customer customer = getCustomerByUsername(username);
        if (request.getFirstName() != null)
            customer.setFirstName(request.getFirstName());
        if (request.getLastName() != null)
            customer.setLastName(request.getLastName());
        if (request.getEmail() != null)
            customer.setEmail(request.getEmail());
        if (request.getPhoneNumber() != null)
            customer.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddress() != null)
            customer.setAddress(request.getAddress());
        return customerRepository.save(customer);
    }
}
