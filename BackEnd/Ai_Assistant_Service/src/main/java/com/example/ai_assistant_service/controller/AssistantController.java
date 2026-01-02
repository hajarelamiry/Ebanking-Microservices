package com.example.ai_assistant_service.controller;

import com.example.ai_assistant_service.dto.ChatRequest;
import com.example.ai_assistant_service.dto.ChatResponse;
import com.example.ai_assistant_service.model.ConversationLog;
import com.example.ai_assistant_service.repository.ConversationLogRepository;
import com.example.ai_assistant_service.service.ChatService;
import com.google.cloud.dialogflow.v2.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AssistantController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ConversationLogRepository logRepository; // Placé ici, au niveau de la classe

    @PostMapping("/chat")
    public ChatResponse processChat(@RequestBody ChatRequest request) {
        ChatResponse chatResponse = new ChatResponse();

        // 1. Appel Dialogflow pour détecter l'intention
        QueryResult queryResult = chatService.detectIntent(request.getMessage(), request.getUserId());

        // 2. Logique de décision : Dialogflow ou Groq ?
        if (queryResult == null || queryResult.getIntent().getDisplayName().equalsIgnoreCase("Default Fallback Intent")) {
            // Appel Groq (IA générative)
            String groqText = chatService.callMistral(request.getMessage());
            chatResponse.setSource("Groq AI (Llama 3)");
            chatResponse.setResponse(groqText);
            chatResponse.setIntent("Fallback");
        } else {
            // Réponse officielle de Dialogflow
            chatResponse.setSource("Dialogflow");
            chatResponse.setResponse(queryResult.getFulfillmentText());
            chatResponse.setIntent(queryResult.getIntent().getDisplayName());
        }

        // 3. Sauvegarde automatique dans MongoDB pour l'historique
        try {
            ConversationLog log = new ConversationLog(
                    request.getUserId(),
                    request.getMessage(),
                    chatResponse.getResponse(),
                    chatResponse.getSource(),
                    chatResponse.getIntent()
            );
            logRepository.save(log);
            System.out.println("DEBUG: Log MongoDB enregistré pour l'utilisateur " + request.getUserId());
        } catch (Exception e) {
            System.err.println("ERREUR MongoDB: Impossible de sauvegarder le log. Vérifiez si MongoDB est lancé.");
        }

        return chatResponse;
    }
}