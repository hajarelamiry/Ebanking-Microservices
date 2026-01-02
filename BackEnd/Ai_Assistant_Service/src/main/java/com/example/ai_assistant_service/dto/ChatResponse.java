package com.example.ai_assistant_service.dto;

import lombok.Data;

@Data
public class ChatResponse {
    private String source; // "Dialogflow" or "Mistral"
    private String response;
    private String intent;
}