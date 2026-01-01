package com.example.dto;

public class NotificationResponse {
    private String status;   // OK / FAILED
    private String details;  // message

    public NotificationResponse() {}

    public NotificationResponse(String status, String details) {
        this.status = status;
        this.details = details;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
