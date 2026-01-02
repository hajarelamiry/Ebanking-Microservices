package com.example.ai_assistant_service.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "ai_conversations") // Nom de la table dans MongoDB
public class ConversationLog {
    @Id
    private String id;
    private String userId;
    private String userMessage;
    private String aiResponse;
    private String source; // Dialogflow ou Groq
    private String intent;
    private LocalDateTime timestamp;

    public ConversationLog(String userId, String userMessage, String aiResponse, String source, String intent) {
        this.userId = userId;
        this.userMessage = userMessage;
        this.aiResponse = aiResponse;
        this.source = source;
        this.intent = intent;
        this.timestamp = LocalDateTime.now();
    }
}