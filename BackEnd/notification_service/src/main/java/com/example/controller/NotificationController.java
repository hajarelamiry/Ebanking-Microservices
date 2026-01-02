package com.example.controller;


import com.example.dto.NotificationRequest;
import com.example.dto.NotificationResponse;
import com.example.service.NotificationSenderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationSenderService senderService;

    public NotificationController(NotificationSenderService senderService) {
        this.senderService = senderService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> send(@RequestBody NotificationRequest request) {
        NotificationResponse response = senderService.send(request);
        if ("FAILED".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("notification-service is UP");
    }
}
