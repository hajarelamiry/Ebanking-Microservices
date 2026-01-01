# Configuration OAuth2 Keycloak - Tous les Microservices

## ✅ Configuration Standardisée

Tous les microservices sont maintenant configurés avec la même configuration OAuth2 Keycloak :

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/ebanking-realm
```

**Note** : `jwk-set-uri` n'est pas nécessaire car Spring Security peut le déduire automatiquement à partir de `issuer-uri`.

## 📋 Services Configurés

### ✅ User Service (port 8082)
- **Fichier** : `user-service/src/main/resources/application.yml`
- **Configuration** : ✅ Port 8080 (corrigé)
- **Dépendance** : ✅ `spring-boot-starter-oauth2-resource-server`

### ✅ Account Service (port 8087)
- **Fichier** : `account-service/src/main/resources/application.properties`
- **Configuration** : ✅ Port 8080
- **Dépendance** : ✅ `spring-boot-starter-oauth2-resource-server`

### ✅ Payment Service (port 8086)
- **Fichier** : `payment-service/src/main/resources/application.properties`
- **Configuration** : ✅ Port 8080
- **Dépendance** : ✅ `spring-boot-starter-oauth2-resource-server`

### ✅ Crypto Service (port 8085)
- **Fichier** : `crypto-service/src/main/resources/application.properties`
- **Configuration** : ✅ Port 8080
- **Dépendance** : ✅ `spring-boot-starter-oauth2-resource-server`

### ✅ Audit Service (port 8084)
- **Fichier** : `audit-service/src/main/resources/application.properties`
- **Configuration** : ✅ Port 8080
- **Dépendance** : ✅ `spring-boot-starter-oauth2-resource-server`

### ✅ Auth Service (port 8081)
- **Fichier** : `auth-service/src/main/resources/application.yml`
- **Configuration** : ✅ Port 8080 (corrigé depuis 8180)
- **Dépendance** : ✅ `spring-boot-starter-oauth2-resource-server`

### ✅ API Gateway (port 8088)
- **Fichier** : `api-gateway/src/main/resources/application.properties`
- **Configuration** : ✅ Port 8080
- **Dépendance** : ✅ `spring-boot-starter-oauth2-resource-server` (ajoutée)

## 🔧 Configuration Keycloak

### Realm
- **Nom** : `ebanking-realm`
- **URL** : `http://localhost:8080/realms/ebanking-realm`

### Client
- **Client ID** : `ebanking-client`
- **Type** : Public client (pas de secret)

### Endpoints
- **Token** : `http://localhost:8080/realms/ebanking-realm/protocol/openid-connect/token`
- **JWK Set** : `http://localhost:8080/realms/ebanking-realm/protocol/openid-connect/certs`

## ✅ Vérification

Tous les services :
1. ✅ Ont la dépendance `spring-boot-starter-oauth2-resource-server`
2. ✅ Ont la configuration `issuer-uri` pointant vers `http://localhost:8080/realms/ebanking-realm`
3. ✅ `jwk-set-uri` n'est pas nécessaire (déduit automatiquement par Spring Security)
4. ✅ Sont enregistrés dans Eureka
5. ✅ Utilisent l'API Gateway comme point d'entrée

## 📝 Note

L'API Gateway peut également être configuré pour valider les tokens JWT, mais généralement les microservices valident directement les tokens pour des raisons de performance.
