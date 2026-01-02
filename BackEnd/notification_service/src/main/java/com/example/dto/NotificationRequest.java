package com.example.dto;

public class NotificationRequest {
    private String userId;
    private String channel; // EMAIL, SMS, PUSH
    private String to;      // email ou numéro ou token push
    private String subject; // pour EMAIL
    private String message;

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
