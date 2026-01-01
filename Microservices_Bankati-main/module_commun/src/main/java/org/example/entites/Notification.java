package org.example.entites;

import jakarta.persistence.*;
import lombok.Data;
import org.example.enums.NotificationType;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private NotificationType type; // EMAIL, SMS, PUSH

    @Column(nullable = false)
    private LocalDateTime date = LocalDateTime.now();
}

