package com.example.demo.client.dto;

import lombok.Data;

@Data
public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String kycStatus;
}
