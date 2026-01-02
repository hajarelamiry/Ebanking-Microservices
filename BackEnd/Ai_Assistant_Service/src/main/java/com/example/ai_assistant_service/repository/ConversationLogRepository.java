package com.example.ai_assistant_service.repository;


import com.example.ai_assistant_service.model.ConversationLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ConversationLogRepository extends MongoRepository<ConversationLog, String> {
}