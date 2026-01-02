package com.example.ai_assistant_service.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String message;
    private String userId;
}