package com.example.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static AuthenticatedUser getCurrentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
            !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }

        String userId = jwt.getSubject(); // sub (UUID)
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        Set<String> roles = extractRoles(jwt);

        return new AuthenticatedUser(
                userId,
                username,
                email,
                firstName,
                lastName,
                roles
        );
    }

    /**
     * Convertit le userId (String UUID) en Long stable pour les services qui en ont besoin
     * Utilise un hash SHA-256 pour garantir la stabilité
     */
    public static Long getUserIdAsLong() {
        AuthenticatedUser user = getCurrentUser();
        String userId = user.getUserId();
        if (userId == null) {
            userId = user.getUsername(); // Fallback sur username
        }
        if (userId == null) {
            return null;
        }
        
        // Si c'est déjà un nombre, le parser directement
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            // Sinon, générer un Long stable à partir du string (UUID ou username)
            return generateStableLongFromString(userId);
        }
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

    private static Set<String> extractRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return Collections.emptySet();
        }

        @SuppressWarnings("unchecked")
        var roles = ((java.util.Map<String, Object>) realmAccess)
                .get("roles");

        if (roles == null) {
            return Collections.emptySet();
        }

        return ((java.util.Collection<String>) roles)
                .stream()
                .collect(Collectors.toSet());
    }
}
