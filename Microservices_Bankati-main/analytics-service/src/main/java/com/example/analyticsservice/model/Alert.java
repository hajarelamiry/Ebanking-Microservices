package com.example.analyticsservice.model;

import java.time.LocalDate;

public class Alert {
    private String alertMessage;
    private LocalDate alertDate;
    private boolean status;

    // 1. Add a No-Args Constructor (Required for JSON frameworks)
    public Alert() {
    }

    // 2. Add the All-Args Constructor (This fixes the compilation error)
    public Alert(String alertMessage, LocalDate alertDate, boolean status) {
        this.alertMessage = alertMessage;
        this.alertDate = alertDate;
        this.status = status;
    }

    // Getters et Setters
    public String getAlertMessage() {
        return alertMessage;
    }

    public void setAlertMessage(String alertMessage) {
        this.alertMessage = alertMessage;
    }

    public LocalDate getAlertDate() {
        return alertDate;
    }

    public void setAlertDate(LocalDate alertDate) {
        this.alertDate = alertDate;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}