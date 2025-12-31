package com.ebanking.user.controller;

import com.ebanking.user.model.Customer;
import com.ebanking.user.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CustomerControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CustomerService customerService;

        @Test
        public void testGetMyProfile_Unauthorized() throws Exception {
                mockMvc.perform(get("/api/customers/me"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        public void testGetMyProfile_Authorized() throws Exception {
                // Mock service response
                when(customerService.getCustomerByUsername(anyString()))
                                .thenReturn(Customer.builder().username("user1").email("user1@test.com").build());

                // Perform request with JWT
                mockMvc.perform(get("/api/customers/me")
                                .with(jwt().jwt(builder -> builder.claim("preferred_username", "user1")
                                                .claim("email", "user1@test.com")
                                                .claim("given_name", "User")
                                                .claim("family_name", "One"))))
                                .andExpect(status().isOk());
        }

        @Test
        public void testGetAllCustomers_Admin_Authorized() throws Exception {
                mockMvc.perform(get("/api/customers")
                                .with(jwt().authorities(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                "ROLE_ADMIN"))))
                                .andExpect(status().isOk());
        }

        @Test
        public void testGetAllCustomers_User_Forbidden() throws Exception {
                mockMvc.perform(get("/api/customers")
                                .with(jwt().authorities(
                                                new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                "ROLE_USER"))))
                                .andExpect(status().isForbidden());
        }

}
