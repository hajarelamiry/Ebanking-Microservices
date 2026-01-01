package com.example.service_utilisateur.dto;

import com.example.service_utilisateur.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterUserDto {
    private String email;

    private String password;

    private String fullName;
    private Role role;

    private String nrCompteBancaire;

    // getters and setters here...
}