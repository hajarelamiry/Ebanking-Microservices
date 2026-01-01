package org.example.dto;

import lombok.Data;
import org.example.enums.NotificationType;

import java.time.LocalDateTime;

@Data
public class NotificationDTO {
    private Long id;
    private Long utilisateurId;
    private String message;
    private NotificationType type; // EMAIL, SMS, PUSH
    private LocalDateTime date;
}