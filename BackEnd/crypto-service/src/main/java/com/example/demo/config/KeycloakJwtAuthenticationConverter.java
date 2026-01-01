package com.example.demo.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realmAccessClaim = jwt.getClaims().get("realm_access");

        if (!(realmAccessClaim instanceof Map)) {
            return List.of();
        }

        Map<String, Object> realmAccess = (Map<String, Object>) realmAccessClaim;
        Object rolesClaim = realmAccess.get("roles");

        if (!(rolesClaim instanceof Collection)) {
            return List.of();
        }

        Collection<String> roles = (Collection<String>) rolesClaim;

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}
