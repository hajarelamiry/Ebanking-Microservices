package com.example.account_service.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertDto {

    private String message;
    private boolean success;
    private LocalDateTime timestamp;
}
