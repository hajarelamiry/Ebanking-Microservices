# Architecture de Communication - API Gateway + Eureka

## ✅ Fichiers Supprimés (Connexions Directes Inutiles)

### Crypto Service
- ❌ `UserClient.java` - Plus utilisé (on utilise directement le JWT)
- ❌ `UserService.java` - Plus utilisé
- ❌ `UserInfoResponse.java` - DTO non utilisé

### Account Service
- ❌ `UserClient.java` - Non utilisé

## ⚠️ Clients Feign Conservés (Communication Inter-Services)

### Pourquoi ces clients sont nécessaires ?

Les services doivent communiquer entre eux pour fonctionner :
- **crypto-service** → **account-service** : Vérifier les soldes, débiter/créditer les comptes
- **payment-service** → **account-service** : Vérifier les soldes, effectuer les virements
- **crypto-service** → **audit-service** : Enregistrer les transactions crypto
- **payment-service** → **audit-service** : Enregistrer les paiements

### Clients Actifs

#### Crypto Service
- ✅ `AccountClient.java` - **NÉCESSAIRE** : Communication avec account-service
- ✅ `AuditClient.java` - **NÉCESSAIRE** : Communication avec audit-service
- ✅ `FeignConfig.java` - **NÉCESSAIRE** : Propagation du JWT entre services

#### Payment Service
- ✅ `AccountClient.java` - **NÉCESSAIRE** : Communication avec account-service
- ✅ `AuditClient.java` - **NÉCESSAIRE** : Communication avec audit-service
- ✅ `FeignConfig.java` - **NÉCESSAIRE** : Propagation du JWT entre services

## 📋 Architecture Actuelle

```
┌─────────────┐
│   Client    │
│  (Browser)  │
└──────┬──────┘
       │ HTTP
       ▼
┌─────────────┐
│ API Gateway │ ← Point d'entrée unique (port 8088)
│  (Eureka)   │
└──────┬──────┘
       │
       ├───► user-service (port 8082)
       ├───► account-service (port 8087)
       ├───► payment-service (port 8086)
       ├───► crypto-service (port 8085)
       ├───► audit-service (port 8084)
       └───► auth-service (port 8083)

Communication Inter-Services (via Eureka + Feign) :
┌─────────────┐         ┌─────────────┐
│crypto-service│────────▶│account-service│
│             │  Feign  │             │
└─────────────┘         └─────────────┘
       │
       ▼
┌─────────────┐
│audit-service│
└─────────────┘
```

## 🔄 Option : Tout Passer par API Gateway

Si vous voulez supprimer TOUS les clients Feign et faire passer TOUT par l'API Gateway :

### Avantages
- ✅ Architecture plus simple (un seul point d'entrée)
- ✅ Pas de dépendances Feign
- ✅ Tous les appels passent par le Gateway

### Inconvénients
- ❌ Performance réduite (un hop supplémentaire)
- ❌ Refonte majeure nécessaire
- ❌ Plus de latence pour les communications inter-services

### Implémentation (si souhaité)

Remplacer les appels Feign par des appels HTTP vers l'API Gateway :

```java
// Au lieu de :
@FeignClient(name = "account-service")
AccountClient accountClient;

// Utiliser :
@Autowired
WebClient.Builder webClientBuilder;

public void callAccountService() {
    webClientBuilder.build()
        .get()
        .uri("http://localhost:8088/api/accounts/...")
        .header("Authorization", "Bearer " + jwtToken)
        .retrieve()
        .bodyToMono(AccountDto.class)
        .block();
}
```

## 📝 Recommandation

**Garder l'architecture actuelle** car :
1. ✅ Les clients externes passent par l'API Gateway (sécurité, point d'entrée unique)
2. ✅ Les services communiquent directement via Eureka/Feign (performance optimale)
3. ✅ Architecture standard pour les microservices

## ✅ Configuration Vérifiée

- ✅ API Gateway configuré avec Eureka
- ✅ Toutes les routes configurées (0-5)
- ✅ Load balancing activé (`lb://`)
- ✅ Discovery locator activé
- ✅ Services inutiles supprimés
