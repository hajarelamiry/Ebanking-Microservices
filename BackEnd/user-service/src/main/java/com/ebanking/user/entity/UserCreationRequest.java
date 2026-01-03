package com.ebanking.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_creation_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private String agentId;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }
}
