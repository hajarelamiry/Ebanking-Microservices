# Comparaison des Configurations - audit-service vs account-service

## Différences Identifiées

### 1. SecurityConfig.java

#### account-service (FONCTIONNE)
```java
@Configuration
@EnableWebSecurity
// PAS de @EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/accounts/**").authenticated()
                .anyRequest().permitAll())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}
```

#### audit-service (PROBLÈME)
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← DIFFÉRENCE 1
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/audit/events", "/api/audit/events/external", "/api/audit/log").permitAll()
                .requestMatchers("/api/audit/users/**/history").hasAnyRole("CLIENT", "AGENT", "ADMIN")
                // ... plus de règles
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));  // ← DIFFÉRENCE 2
        return http.build();
    }
    
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtAuthenticationConverter());
        return converter;
    }
}
```

### 2. application.properties

#### account-service
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/ebanking-realm
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/ebanking-realm/protocol/openid-connect/certs
# PAS de logging Spring Security
```

#### audit-service
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/ebanking-realm  # ← PORT DIFFÉRENT
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/realms/ebanking-realm/protocol/openid-connect/certs
logging.level.org.springframework.security=DEBUG  # ← LOGGING ACTIVÉ
```

### 3. KeycloakJwtAuthenticationConverter

- **account-service** : N'utilise PAS ce converter (utilise Customizer.withDefaults())
- **audit-service** : Utilise KeycloakJwtAuthenticationConverter (identique à payment et crypto)

## Analyse

### Points Clés

1. **account-service utilise `Customizer.withDefaults()`** : Configuration par défaut de Spring Security qui gère mieux les requêtes sans token
2. **audit-service utilise `KeycloakJwtAuthenticationConverter`** : Configuration personnalisée pour les rôles Keycloak
3. **Port Keycloak différent** : account-service utilise 8180, audit-service utilise 8080
4. **@EnableMethodSecurity** : Présent dans audit-service, absent dans account-service

### Problème Identifié

Le problème pourrait venir du fait que :
- Spring Security essaie de valider le JWT même quand il n'y a pas de token
- Avec `KeycloakJwtAuthenticationConverter`, Spring Security pourrait essayer d'accéder à Keycloak pour valider
- Si Keycloak n'est pas accessible, cela lève une exception 500 au lieu de retourner 401

### Solution Proposée

1. **Option 1** : Utiliser la même configuration que account-service (Customizer.withDefaults()) mais cela perdrait la gestion des rôles Keycloak
2. **Option 2** : Vérifier que Keycloak est accessible sur le port 8080
3. **Option 3** : Configurer Spring Security pour qu'il ne tente pas de valider le JWT quand il n'y a pas de token

## Recommandation

Comme payment-service et crypto-service fonctionnent avec KeycloakJwtAuthenticationConverter, le problème n'est probablement pas le converter lui-même, mais plutôt :
1. La connexion à Keycloak (port 8080 vs 8180)
2. La configuration Spring Security qui essaie de valider même sans token
