package com.example.service_utilisateur.service;

import com.example.service_utilisateur.client.CmiClient;
import com.example.service_utilisateur.dto.LoginUserDto;
import com.example.service_utilisateur.dto.RegisterUserDto;
import com.example.service_utilisateur.model.Role;
import com.example.service_utilisateur.model.User;
import com.example.service_utilisateur.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    @Autowired
    private CmiClient cmiClient;
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UserRepository userRepository,
            AuthenticationManager authenticationManager,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(RegisterUserDto input) {
        User user = new User();
        user.setFullName(input.getFullName());
        user.setEmail(input.getEmail());
        user.setPassword(passwordEncoder.encode(input.getPassword()));
        user.setRole(input.getRole());
        User savedUser=userRepository.save(user);
        if(user.getRole() == Role.CLIENT){
            this.cmiClient.assignerUtilisateur(input.getNrCompteBancaire(),savedUser.getId());
        }
        return savedUser;
    }

    public User authenticate(LoginUserDto input) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        input.getEmail(),
                        input.getPassword()
                )
        );

        return userRepository.findByEmail(input.getEmail())
                .orElseThrow();
    }
}