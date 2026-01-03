package com.ebanking.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import com.example.common.security.KeycloakJwtAuthenticationConverter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/**", "/graphiql/**", "/graphql", "/graphql/**", "/graphiql")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder(
            org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties properties) {
        String jwkSetUri = properties.getJwt().getJwkSetUri();
        org.springframework.security.oauth2.jwt.NimbusJwtDecoder jwtDecoder = org.springframework.security.oauth2.jwt.NimbusJwtDecoder
                .withJwkSetUri(jwkSetUri).build();

        org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> validator = new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                new org.springframework.security.oauth2.jwt.JwtTimestampValidator());
        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    @Bean
    public org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter jwtAuthenticationConverter() {
        org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter converter = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtAuthenticationConverter());
        return converter;
    }
}
