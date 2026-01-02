package com.example.ai_assistant_service.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.dialogflow.v2.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.util.Collections;

@Service
public class ChatService {

    @Value("${dialogflow.project-id}")
    private String projectId;

    @Value("${google.application.credentials}")
    private String credentialsPath;

    @Value("${mistral.api.key}")
    private String mistralKey;

    @Value("${mistral.api.url}")
    private String mistralUrl;

    private SessionsClient sessionsClient;

    @PostConstruct
    public void init() throws Exception {
        System.out.println("--- Booting Dialogflow Service ---");

        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                .createScoped(Collections.singleton("https://www.googleapis.com/auth/cloud-platform"));

        // FIXED: Add a retry/timeout configuration to stop the "hanging"
        SessionsSettings sessionsSettings = SessionsSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .setEndpoint("dialogflow.googleapis.com:443")
                .build();

        this.sessionsClient = SessionsClient.create(sessionsSettings);
        System.out.println("--- Dialogflow Service Ready ---");
    }

    public String callMistral(String userMessage) {
        System.out.println("Calling Groq AI (Llama 3)...");

        // Add timeouts
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10 seconds
        factory.setReadTimeout(10000);
        RestTemplate restTemplate = new RestTemplate(factory);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(mistralKey); // This now uses your Groq Key

        // Groq / OpenAI Standard Format
        JSONObject body = new JSONObject();
        body.put("model", "llama-3.3-70b-versatile"); // One of Groq's best free models

        JSONArray messages = new JSONArray();

        // System message to tell the AI how to behave
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a professional E-Banking Assistant for the E-Banking 3.0 platform. Be helpful and concise.");
        messages.put(systemMessage);

        // User message
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.put(userMsg);

        body.put("messages", messages);

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(mistralUrl, entity, String.class);
            JSONObject responseJson = new JSONObject(response.getBody());

            // Extracting the text response from Groq/OpenAI format
            String aiResult = responseJson.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            System.out.println("Groq AI Response Received.");
            return aiResult;
        } catch (Exception e) {
            System.err.println("Groq API Error: " + e.getMessage());
            return "I'm sorry, my generative AI engine is currently resting. How else can I help you?";
        }
    }

    public QueryResult detectIntent(String text, String sessionId) {
        System.out.println("Step 1: Contacting Dialogflow for message: " + text);
        try {
            SessionName session = SessionName.of(projectId, sessionId);
            TextInput.Builder textInput = TextInput.newBuilder().setText(text).setLanguageCode("en-US");
            QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

            // This is where it usually hangs if the network/time is bad
            DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);

            System.out.println("Step 2: Dialogflow responded with intent: " + response.getQueryResult().getIntent().getDisplayName());
            return response.getQueryResult();
        } catch (Exception e) {
            System.err.println("Dialogflow Connection Failed: " + e.getMessage());
            return null; // Fallback to Mistral
        }
    }
}