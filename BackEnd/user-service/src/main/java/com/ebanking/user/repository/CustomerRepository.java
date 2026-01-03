package com.ebanking.user.repository;

import com.ebanking.user.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByKeycloakId(String keycloakId);
}
