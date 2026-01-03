package com.ebanking.user.repository;

import com.ebanking.user.entity.UserCreationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserCreationRequestRepository extends JpaRepository<UserCreationRequest, Long> {
    List<UserCreationRequest> findByStatus(UserCreationRequest.RequestStatus status);
}
