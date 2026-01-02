package com.example.analyticsservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "expenses")
public class Expense {
    @Id
    private String id;  // MongoDB utilise String par défaut pour les IDs
    private Double amount;
    private String category;
    private String description;
    private String portefeuilleId;

    // Getters et Setters

    public Double getAmount() {
        return amount;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getPortefeuilleId() {
        return portefeuilleId;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPortefeuilleId(String portefeuilleId) {
        this.portefeuilleId = portefeuilleId;
    }
}
