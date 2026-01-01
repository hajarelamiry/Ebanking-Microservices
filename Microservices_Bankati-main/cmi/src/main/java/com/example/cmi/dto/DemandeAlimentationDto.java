package com.example.cmi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
public class DemandeAlimentationDto {
    private Long userId;
    private Long portefeuilleId;
    private Double montant;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getPortefeuilleId() {
        return portefeuilleId;
    }

    public void setPortefeuilleId(Long portefeuilleId) {
        this.portefeuilleId = portefeuilleId;
    }

    public Double getMontant() {
        return montant;
    }

    public void setMontant(Double montant) {
        this.montant = montant;
    }
}
