package com.example.demo.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class JwtUtils {
    
    /**
     * Récupère l'identifiant utilisateur depuis le JWT
     * Priorité : sub (UUID Keycloak) > preferred_username
     * Utilisé pour générer un Long userId stable
     */
    public static String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            // Keycloak stocke généralement le userId dans "sub" (UUID unique) ou "preferred_username"
            String sub = jwt.getClaimAsString("sub");
            if (sub != null && !sub.isEmpty()) {
                return sub;
            }
            return jwt.getClaimAsString("preferred_username");
        }
        return null;
    }
    
    /**
     * Récupère le username (preferred_username) depuis le JWT
     * Utilisé pour les appels à account-service qui attend un username
     */
    public static String getUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            // Account-service utilise preferred_username (ex: "user1")
            return jwt.getClaimAsString("preferred_username");
        }
        return null;
    }

    /**
     * Récupère l'identifiant utilisateur comme Long depuis le JWT
     * Utilise le "sub" (UUID) du JWT et génère un Long stable via hash
     * Si le "sub" n'est pas disponible, utilise preferred_username
     */
    public static Long getUserIdAsLong() {
        String userId = getUserId();
        if (userId != null) {
            // Si c'est déjà un nombre, le parser directement
            try {
                return Long.parseLong(userId);
            } catch (NumberFormatException e) {
                // Sinon, générer un Long stable à partir du string (sub UUID ou username)
                // Utilise un hash pour garantir la stabilité
                return generateStableLongFromString(userId);
            }
        }
        return null;
    }
    
    /**
     * Génère un Long stable à partir d'un String (UUID ou username)
     * Utilise SHA-256 pour garantir la stabilité et l'unicité
     */
    private static Long generateStableLongFromString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            // Prendre les 8 premiers bytes pour créer un Long
            long result = 0;
            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (hash[i] & 0xFF);
            }
            // S'assurer que c'est positif (enlever le bit de signe)
            return Math.abs(result);
        } catch (NoSuchAlgorithmException e) {
            // Fallback : utiliser le hashcode
            return Math.abs((long) input.hashCode());
        }
    }

    public static boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
        }
        return false;
    }

    public static boolean isClient() {
        return hasRole("CLIENT");
    }

    public static boolean isAgent() {
        return hasRole("AGENT");
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
