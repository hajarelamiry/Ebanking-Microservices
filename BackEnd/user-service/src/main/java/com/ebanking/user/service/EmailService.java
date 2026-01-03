package com.ebanking.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendCredentials(String to, String firstName, String username, String password) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("saida.hourani12@gmail.com");
        message.setTo(to);
        message.setSubject("Bienvenue chez Capitalis - Vos accès");
        message.setText("Bonjour " + firstName + ",\n\n" +
                "Votre compte bancaire Capitalis a été créé avec succès par notre équipe.\n\n" +
                "Voici vos identifiants de connexion :\n" +
                "Nom d'utilisateur : " + username + "\n" +
                "Mot de passe temporaire : " + password + "\n\n" +
                "Veuillez vous connecter sur http://localhost:4200 pour activer votre compte.\n" +
                "Il vous sera demandé de changer ce mot de passe lors de votre première connexion.\n\n" +
                "Cordialement,\n" +
                "L'équipe Capitalis");
        try {
            mailSender.send(message);
            System.out.println("Email sent successfully to " + to);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            // Do not rethrow exception to avoid rollback of user creation
        }
    }
}
