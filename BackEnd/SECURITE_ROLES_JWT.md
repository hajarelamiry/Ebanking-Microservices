# Sécurité et Gestion des Rôles avec JWT/Keycloak

## 📋 Vue d'ensemble

Intégration de la sécurité JWT via Keycloak dans les services Payment, Crypto et Audit avec gestion des rôles (CLIENT, AGENT, ADMIN).

---

## 🔐 Configuration Keycloak

### URL de Configuration
- **Keycloak** : `http://localhost:8080`
- **Realm** : `ebanking-realm`
- **Issuer URI** : `http://localhost:8080/realms/ebanking-realm`

### Rôles Disponibles
- `CLIENT` : Utilisateur final
- `AGENT` : Agent bancaire
- `ADMIN` : Administrateur système

---

## 💳 Payment Service - `/api/v1/payments`

### POST `/api/v1/payments`
**Rôles autorisés** : `CLIENT`, `AGENT`, `ADMIN`

- **CLIENT** : Crée un virement pour lui-même
- **AGENT** : Peut créer un virement assisté (agence)
- **ADMIN** : Accès complet pour supervision et tests

**Configuration** :
- Tous les endpoints `/api/v1/payments/**` nécessitent l'un des rôles : CLIENT, AGENT, ADMIN

---

## 🪙 Crypto Service - `/api/v1/crypto`

### GET `/api/v1/crypto/prices`
**Rôles autorisés** : `CLIENT`, `AGENT`, `ADMIN`
- Consultation des prix des cryptomonnaies (lecture seule)

### GET `/api/v1/crypto/wallet?userId=`
**Rôles autorisés** : `CLIENT`, `AGENT`, `ADMIN`
- **CLIENT** : Peut consulter uniquement son propre wallet
- **AGENT/ADMIN** : Peut consulter tous les wallets
- Si `userId` n'est pas fourni, utilise celui du token JWT

### POST `/api/v1/crypto/trade`
**Rôles autorisés** : `CLIENT` uniquement
- Achat ou vente de cryptomonnaies
- **Interdit aux AGENT et ADMIN** pour des raisons de sécurité et de conformité
- Le CLIENT ne peut trader que pour lui-même

### GET `/api/v1/crypto/history?userId=`
**Rôles autorisés** : `CLIENT`, `AGENT`, `ADMIN`
- **CLIENT** : Voit uniquement son propre historique
- **AGENT/ADMIN** : Accès complet à tous les historiques
- Si `userId` n'est pas fourni, utilise celui du token JWT

---

## 📊 Audit Service - `/api/audit`

### Endpoints Internes (Sans authentification utilisateur)
Ces endpoints sont accessibles uniquement via Feign/Eureka pour les microservices internes :

- `POST /api/audit/events`
- `POST /api/audit/events/external`
- `POST /api/audit/log`

**Rôles** : Aucun rôle utilisateur requis (accès interne uniquement)

### GET `/api/audit/users/{userId}/history`
**Rôles autorisés** : `CLIENT`, `AGENT`, `ADMIN`
- **CLIENT** : Accède uniquement à son propre historique
- **AGENT/ADMIN** : Peuvent consulter l'historique de tous les utilisateurs

### GET `/api/audit/history`
**Rôles autorisés** : `ADMIN` uniquement
- Accès global à tous les audits du système (sécurité, conformité, RGPD)

### GET `/api/audit/errors`
**Rôles autorisés** : `AGENT`, `ADMIN`
- Consultation des erreurs, échecs et incidents techniques

### GET `/api/audit/stats/user/{userId}`
**Rôles autorisés** : `CLIENT`, `AGENT`, `ADMIN`
- Statistiques d'actions par utilisateur
- **CLIENT** : Limité à ses propres statistiques

### GET `/api/audit/stats/errors`
**Rôles autorisés** : `ADMIN` uniquement
- Statistiques globales des erreurs du système

### GET `/api/audit/health`
**Rôles autorisés** : `CLIENT`, `AGENT`, `ADMIN`
- Health check du service

---

## 🛠️ Implémentation Technique

### Dépendances Ajoutées
```xml
<!-- Spring Security OAuth2 Resource Server -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

### Fichiers Créés

#### Configuration de Sécurité
- `SecurityConfig.java` : Configuration Spring Security avec Keycloak
- `KeycloakJwtAuthenticationConverter.java` : Conversion des rôles Keycloak en authorities Spring

#### Utilitaires
- `JwtUtils.java` : Utilitaires pour extraire userId et vérifier les rôles depuis le token JWT

### Annotations Utilisées
- `@PreAuthorize("hasRole('ROLE')")` : Vérification des rôles au niveau méthode
- `@EnableMethodSecurity` : Activation de la sécurité au niveau méthode

---

## 🔑 Extraction du UserId

Le `userId` est extrait du token JWT depuis :
1. Le claim `sub` (subject) - prioritaire
2. Le claim `preferred_username` - en fallback

**Exemple d'utilisation** :
```java
String userId = JwtUtils.getUserId();
Long userIdLong = JwtUtils.getUserIdAsLong();
```

---

## 🚨 Gestion des Accès

### Vérifications Implémentées

1. **Vérification des rôles** : Via `@PreAuthorize`
2. **Vérification du userId** : 
   - CLIENT ne peut accéder qu'à ses propres données
   - AGENT et ADMIN ont accès complet
3. **Exceptions** : `AccessDeniedException` si accès refusé

### Exemple de Code
```java
// Vérifier que le CLIENT ne peut accéder qu'à son propre wallet
if (JwtUtils.isClient() && !targetUserId.equals(JwtUtils.getUserIdAsLong())) {
    throw new AccessDeniedException("CLIENT can only access their own wallet");
}
```

---

## 📝 Configuration Application Properties

### Payment Service
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/ebanking-realm
```

### Crypto Service
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/ebanking-realm
```

### Audit Service
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/ebanking-realm
```

---

## 🧪 Test avec Postman

### 1. Obtenir un Token JWT
Appeler l'endpoint d'authentification Keycloak pour obtenir un token :
```
POST http://localhost:8080/realms/ebanking-realm/protocol/openid-connect/token
```

### 2. Utiliser le Token
Ajouter le token dans le header `Authorization` :
```
Authorization: Bearer <token>
```

### 3. Tester les Endpoints
- Tous les endpoints nécessitent maintenant un token JWT valide
- Les rôles sont vérifiés automatiquement

---

## ✅ Checklist de Validation

- [x] Dépendances Spring Security ajoutées
- [x] Configuration Keycloak dans les 3 services
- [x] SecurityConfig créé pour chaque service
- [x] KeycloakJwtAuthenticationConverter créé
- [x] JwtUtils créé pour extraction userId
- [x] Annotations @PreAuthorize ajoutées sur les endpoints
- [x] Vérifications de userId pour CLIENT
- [x] Endpoints internes Audit Service sans authentification
- [x] Documentation complète

---

## 🔄 Prochaines Étapes (optionnel)

1. Ajouter des tests d'intégration pour la sécurité
2. Configurer CORS si nécessaire
3. Ajouter un filtre pour logger les tentatives d'accès
4. Implémenter un cache pour les tokens JWT
