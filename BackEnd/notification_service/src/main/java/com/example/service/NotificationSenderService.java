package com.example.service;

import com.example.dto.NotificationRequest;
import com.example.dto.NotificationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationSenderService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSenderService.class);

    public NotificationResponse send(NotificationRequest request) {
        // Pour commencer: simulation (log)
        if (request.getTo() == null || request.getTo().isBlank()) {
            return new NotificationResponse("FAILED", "Field 'to' is required");
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return new NotificationResponse("FAILED", "Field 'message' is required");
        }

        log.info("NOTIFICATION -> userId={}, channel={}, to={}, subject={}, message={}",
                request.getUserId(),
                request.getChannel(),
                request.getTo(),
                request.getSubject(),
                request.getMessage()
        );

        return new NotificationResponse("OK", "Notification simulated (logged)");
    }
}
