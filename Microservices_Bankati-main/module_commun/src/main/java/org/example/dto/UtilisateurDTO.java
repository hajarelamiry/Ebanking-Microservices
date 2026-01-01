package org.example.dto;

import lombok.Data;
import org.example.enums.Role;

import java.util.List;

@Data
public class UtilisateurDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private Role role; // CLIENT, AGENT, ADMIN
//    private List<PortefeuillesDTO> portefeuilles;
//    private List<CarteVirtuelleDTO> cartes_virtuelles;
//    private List<NotificationDTO> notifications;
}