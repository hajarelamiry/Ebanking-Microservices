package com.ebanking.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class TestController {

    @GetMapping("/public")
    public String publicEndpoint() {
        return "Public Endpoint - Accessible without token";
    }

    @GetMapping("/protected")
    public String protectedEndpoint() {
        return "Protected Endpoint - Requires valid JWT";
    }
}
