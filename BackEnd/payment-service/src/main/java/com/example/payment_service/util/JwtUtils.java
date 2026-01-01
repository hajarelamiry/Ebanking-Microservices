package com.example.payment_service.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class JwtUtils {

    public static String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            // Keycloak stocke généralement le userId dans "sub" ou "preferred_username"
            String sub = jwt.getClaimAsString("sub");
            if (sub != null) {
                return sub;
            }
            return jwt.getClaimAsString("preferred_username");
        }
        return null;
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
