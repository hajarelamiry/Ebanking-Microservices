package com.ebanking.auth.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testPublicEndpoint() throws Exception {
        mockMvc.perform(get("/auth/public"))
                .andExpect(status().isOk())
                .andExpect(content().string("Public Endpoint - Accessible without token"));
    }

    @Test
    public void testProtectedEndpoint_NoToken() throws Exception {
        mockMvc.perform(get("/auth/protected"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    public void testProtectedEndpoint_WithMockUser() throws Exception {
        // Simule un utilisateur authentifié pour tester que l'endpoint accepte la
        // connexion
        mockMvc.perform(get("/auth/protected"))
                .andExpect(status().isOk())
                .andExpect(content().string("Protected Endpoint - Requires valid JWT"));
    }
}
