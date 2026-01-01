# Intégration Inter-Services - Payment, Crypto et Account Services

## Objectif
Mettre en place une communication inter-microservices cohérente et sécurisée, permettant à `payment-service` et `crypto-service` de s'appuyer sur `account-service` pour les contrôles de solde, tout en conservant une architecture découplée basée sur Eureka.

## Architecture

```
┌─────────────────┐         ┌─────────────────┐
│ payment-service │────────▶│ account-service │
│                 │ Feign   │                 │
│                 │ JWT     │                 │
└─────────────────┘         └─────────────────┘
         │                            ▲
         │                            │
         │                            │
         ▼                            │
┌─────────────────┐                  │
│ crypto-service  │──────────────────┘
│                 │ Feign + JWT
└─────────────────┘
```

## Implémentation

### 1. Payment Service

#### Feign Client créé
- **AccountClient** : Interface Feign pour communiquer avec account-service
- **AccountClientFallback** : Gestion des erreurs en cas d'indisponibilité
- **BalanceResponseDTO** : DTO pour la réponse de solde

#### Service créé
- **AccountService** : Service wrapper pour les opérations avec account-service
  - `getBalance(accountRef)` : Récupère le solde d'un compte
  - `hasSufficientBalance(accountRef, amount)` : Vérifie si le solde est suffisant
  - `debitAccount(accountRef, amount)` : Débite un compte

#### Intégration dans PaymentServiceImpl
- **Vérification de solde** : Avant chaque paiement, vérifie que le compte a un solde suffisant
- **Rejet automatique** : Si le solde est insuffisant, le paiement est rejeté avec statut `REJECTED`
- **Audit** : Les rejets pour solde insuffisant sont enregistrés dans audit-service

### 2. Crypto Service

#### Feign Client créé
- **AccountClient** : Interface Feign pour communiquer avec account-service
  - `getBalance(accountRef)` : Récupère le solde
  - `debit(accountRef, amount)` : Débite un compte (pour les achats)
  - `credit(accountRef, amount)` : Crédite un compte (pour les ventes)
- **AccountClientFallback** : Gestion des erreurs
- **BalanceResponseDTO** : DTO pour la réponse de solde

#### Service créé
- **AccountService** : Service wrapper pour les opérations bancaires
  - `getBalance(accountRef)` : Récupère le solde
  - `hasSufficientBalance(accountRef, amount)` : Vérifie le solde
  - `debitAccount(accountRef, amount)` : Débite le compte
  - `creditAccount(accountRef, amount)` : Crédite le compte
  - `getAccountRefByUserId(userId)` : Récupère l'accountRef d'un utilisateur via le nouvel endpoint

#### Intégration dans CryptoTradingService
- **Récupération de l'accountRef** :
  1. Utilise `accountService.getAccountRefByUserId(userId.toString())` pour obtenir l'accountRef
  2. Appelle l'endpoint `GET /api/accounts/user/{userId}` dans account-service
  3. Retourne le compte principal (EUR par défaut, ou premier compte actif)
  
- **Pour les ACHATS (BUY)** :
  1. Récupère l'accountRef via `getAccountRefByUserId`
  2. Calcule le montant total en EUR (quantity * price)
  3. Vérifie le solde du compte bancaire
  4. Débite le compte bancaire
  5. Crédite le wallet crypto
  
- **Pour les VENTES (SELL)** :
  1. Récupère l'accountRef via `getAccountRefByUserId`
  2. Vérifie le solde du wallet crypto
  3. Débite le wallet crypto
  4. Calcule le montant total en EUR (quantity * price)
  5. Crédite le compte bancaire

### 3. Configuration

#### Propagation du Token JWT
- **FeignConfig** créé dans payment-service et crypto-service
- **RequestInterceptor** : Propage automatiquement le token JWT dans les en-têtes des requêtes Feign
- Le token est extrait du `SecurityContextHolder` et ajouté comme `Authorization: Bearer {token}`

#### Resilience4j Circuit Breaker
- Configuration ajoutée pour `accountService` dans les deux services
- Gestion automatique des erreurs et des timeouts
- Fallback en cas d'indisponibilité du service

#### Eureka Service Discovery
- Les services utilisent le nom de service (`account-service`) pour la découverte
- Eureka résout automatiquement l'URL du service

## Points d'Attention

### Mapping userId → accountRef
✅ **Corrigé** : Le mapping `userId → accountRef` est maintenant géré proprement via un endpoint dédié.

**Solution implémentée** : 
- ✅ Endpoint `GET /api/accounts/user/{userId}` créé dans `account-service`
- ✅ Retourne le compte principal (EUR par défaut, ou premier compte actif)
- ✅ `crypto-service` utilise maintenant `accountService.getAccountRefByUserId(userId)` au lieu de `userId.toString()`
- ✅ `payment-service` utilise déjà `sourceAccountId` fourni par l'utilisateur (pas de changement nécessaire)

**Avantages** :
- ✅ Aucune supposition sur le format de l'accountRef
- ✅ Gestion propre des comptes multiples par utilisateur
- ✅ Sécurité : vérification que l'utilisateur demande son propre compte

### Gestion des Erreurs
- Les erreurs de communication avec `account-service` sont gérées par le Circuit Breaker
- Les exceptions sont propagées et les transactions sont rejetées en cas d'erreur

### Transactionalité
- Les opérations dans `CryptoTradingService` sont `@Transactional`
- En cas d'erreur lors du débit/crédit, la transaction est rollback

## Tests

Un script de test a été créé : `test-inter-service-integration.ps1`

Ce script teste :
1. Création d'un compte dans account-service
2. Vérification du solde
3. Crédit du compte
4. Tentative de paiement avec solde insuffisant
5. Tentative de paiement avec solde suffisant
6. Tentative d'achat crypto avec solde insuffisant
7. Tentative d'achat crypto avec solde suffisant

## Prochaines Étapes

1. ✅ Feign Clients créés
2. ✅ Propagation du token JWT configurée
3. ✅ Vérification de solde implémentée
4. ✅ Opérations bancaires intégrées
5. ✅ Mapping userId → accountRef corrigé via endpoint dédié
6. ⏳ Tests d'intégration à exécuter

## Nouveau Endpoint Account Service

### GET /api/accounts/user/{userId}

**Description** : Récupère le compte principal d'un utilisateur par son userId.

**Authentification** : Requiert un token JWT valide. L'utilisateur ne peut demander que son propre compte.

**Réponse** :
```json
{
  "externalReference": "uuid-du-compte",
  "balance": 1000.00,
  "devise": "EUR",
  "status": "ACTIF",
  "createdAt": "2024-01-01T00:00:00"
}
```

**Logique** :
- Cherche d'abord le compte EUR (compte principal)
- Si EUR n'existe pas, retourne le premier compte actif disponible
- Lance une exception si aucun compte n'est trouvé

**Utilisation dans crypto-service** :
```java
String accountRef = accountService.getAccountRefByUserId(userId.toString());
```
