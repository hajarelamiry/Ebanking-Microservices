package com.example.audit_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * SecurityFilterChain pour les endpoints INTERNES
     * Ces endpoints sont accessibles SANS token et même avec token invalide
     * Le filtre JWT n'est PAS appliqué sur ces endpoints
     */
    @Bean
    @Order(1)
    public SecurityFilterChain internalEndpointsFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/audit/log", "/api/audit/events/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                // PAS de oauth2ResourceServer ici - le JWT n'est pas validé
                ;

        return http.build();
    }

    /**
     * SecurityFilterChain pour les endpoints UTILISATEURS
     * Ces endpoints nécessitent un token JWT valide
     * Sans token ou avec token invalide → 401
     * Avec token valide → OK (selon les rôles)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain userEndpointsFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/audit/users/**", "/api/audit/history", "/api/audit/errors", 
                                 "/api/audit/stats/**", "/api/audit/health")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/audit/users/**/history").hasAnyRole("CLIENT", "AGENT", "ADMIN")
                        .requestMatchers("/api/audit/history").hasRole("ADMIN")
                        .requestMatchers("/api/audit/errors").hasAnyRole("AGENT", "ADMIN")
                        .requestMatchers("/api/audit/stats/user/**").hasAnyRole("CLIENT", "AGENT", "ADMIN")
                        .requestMatchers("/api/audit/stats/errors").hasRole("ADMIN")
                        .requestMatchers("/api/audit/health").hasAnyRole("CLIENT", "AGENT", "ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    /**
     * SecurityFilterChain pour les autres endpoints (Swagger, etc.)
     */
    @Bean
    @Order(3)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtAuthenticationConverter());
        return converter;
    }
}
