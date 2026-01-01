# Guide de Test Complet - Integration des Services

## 📋 Vue d'ensemble

Ce guide explique comment tester l'intégration complète des services :
1. **Authentification** via Keycloak
2. **Création/Vérification** du compte dans account-service
3. **Test du mapping** userId -> accountRef
4. **Tests des transactions** crypto
5. **Vérification de la liaison** entre tous les services

## 🚀 Script de Test

### `test-complete-integration.ps1`

Script complet qui teste toute la chaîne d'intégration.

## 📝 Configuration

### Ports des Services

- **Keycloak** : `http://localhost:8080`
- **Auth Service** : `http://localhost:8081`
- **Account Service** : `http://localhost:8087`
- **Crypto Service** : `http://localhost:8085`
- **Payment Service** : `http://localhost:8086`
- **Audit Service** : `http://localhost:8084`
- **User Service** : `http://localhost:8082`

### Identifiants

Modifiez les identifiants dans le script (lignes 25-26) :
```powershell
$testUsername = "client1"  # Votre utilisateur Keycloak
$testPassword = "password"  # Mot de passe
```

## 🧪 Étapes du Test

### Étape 1 : Authentification

Le script s'authentifie directement via Keycloak :
- Endpoint : `POST /realms/ebanking-realm/protocol/openid-connect/token`
- Obtient un token JWT valide
- Vérifie que l'authentification fonctionne

**Résultat attendu** :
- ✅ Token obtenu avec succès

### Étape 2 : Création/Vérification du Compte

Le script vérifie si l'utilisateur a un compte :
- Si le compte existe : le récupère
- Si le compte n'existe pas : le crée avec 1000 EUR

**Résultat attendu** :
- ✅ Compte trouvé ou créé avec succès
- ✅ Account Reference obtenu

### Étape 3 : Test du Mapping userId -> accountRef

Le script teste l'endpoint de mapping :
- `GET /api/accounts/user/{userId}`
- Vérifie que le mapping fonctionne correctement

**Résultat attendu** :
- ✅ Mapping fonctionne
- ✅ accountRef correctement récupéré

### Étape 4 : Récupération des Prix Crypto

Le script récupère les prix crypto depuis crypto-service :
- `GET /api/v1/crypto/prices`
- Obtient le prix BTC en EUR

**Résultat attendu** :
- ✅ Prix récupérés avec succès

### Étape 5 : Test Transaction Crypto (Solde Insuffisant)

Le script teste un achat avec un montant supérieur au solde :
- Doit être rejeté avec une erreur 400/500

**Résultat attendu** :
- ✅ Achat rejeté comme attendu

### Étape 6 : Test Transaction Crypto (Solde Suffisant)

Le script teste un achat avec un montant inférieur au solde :
- Doit réussir
- Le compte doit être débité
- Le wallet crypto doit être crédité

**Résultat attendu** :
- ✅ Achat effectué avec succès
- ✅ Compte débité correctement
- ✅ Solde final vérifié

### Étape 7 : Vérification de la Liaison entre Services

Le script vérifie que tous les appels inter-services fonctionnent :
- crypto-service -> account-service (mapping)
- crypto-service -> account-service (débit)
- crypto-service -> audit-service (audit logging)
- payment-service -> account-service (vérification solde)
- Eureka Service Discovery
- JWT Token Propagation

**Résultat attendu** :
- ✅ Tous les appels inter-services fonctionnent

## 🔍 Vérifications Manuelles

### 1. Vérifier les Logs

**Dans crypto-service**, lors d'une transaction, vous devriez voir :
```
Account reference retrieved for user {username}: {accountRef}
Account {accountRef} debited with {amount} EUR for crypto purchase
```

**Dans account-service**, vous devriez voir :
```
GET /api/accounts/user/{userId}
POST /api/accounts/{accountRef}/debit
```

**Dans audit-service**, vous devriez voir :
```
Audit event received: CRYPTO_BUY
```

### 2. Vérifier Eureka

Accédez à `http://localhost:8761` et vérifiez que tous les services sont enregistrés :
- account-service
- crypto-service
- payment-service
- audit-service

### 3. Vérifier le Mapping

Testez manuellement l'endpoint de mapping :
```powershell
GET http://localhost:8087/api/accounts/user/{userId}
Authorization: Bearer {token}
```

## 🐛 Dépannage

### Erreur : "Invalid user credentials"

**Solution** :
- Vérifiez que les identifiants sont corrects
- Vérifiez que l'utilisateur existe dans Keycloak
- Vérifiez que l'utilisateur a le rôle CLIENT

### Erreur : "Account Service unavailable"

**Solution** :
- Vérifiez que account-service est démarré
- Vérifiez que Eureka peut résoudre le service
- Vérifiez les logs de crypto-service pour l'erreur exacte

### Erreur : "Compte introuvable"

**Solution** :
- Le script créera automatiquement un compte
- Si cela échoue, créez-le manuellement :
  ```powershell
  POST http://localhost:8087/api/accounts
  Authorization: Bearer {token}
  Body: {
    "devise": "EUR",
    "initialBalance": 1000.00
  }
  ```

### Erreur : "401 Unauthorized"

**Solution** :
- Vérifiez que le token JWT est valide
- Vérifiez que le token n'a pas expiré
- Vérifiez la configuration Keycloak

## ✅ Checklist de Vérification

- [ ] Keycloak est démarré sur le port 8080
- [ ] Tous les services sont démarrés
- [ ] Eureka Server est démarré et accessible
- [ ] Les services sont enregistrés dans Eureka
- [ ] L'utilisateur de test existe dans Keycloak
- [ ] L'utilisateur a le rôle CLIENT
- [ ] Les identifiants sont corrects dans le script
- [ ] Le script s'exécute sans erreur
- [ ] Le mapping userId -> accountRef fonctionne
- [ ] Les transactions crypto fonctionnent
- [ ] Le compte est correctement débité/crédité
- [ ] Les événements d'audit sont enregistrés

## 📊 Résumé de l'Intégration

### Architecture

```
┌─────────────┐
│  Keycloak   │ (Authentification)
└──────┬──────┘
       │ JWT Token
       ▼
┌─────────────┐
│ Auth Service │ (Validation JWT)
└──────┬──────┘
       │
       ▼
┌─────────────┐         ┌─────────────┐
│   Client    │────────▶│ Account     │
│             │ Feign   │ Service     │
└──────┬──────┘         └─────────────┘
       │
       │ JWT Token
       ▼
┌─────────────┐         ┌─────────────┐
│   Crypto    │────────▶│ Account     │
│   Service   │ Feign   │ Service     │
└──────┬──────┘         └─────────────┘
       │
       │ Audit Events
       ▼
┌─────────────┐
│   Audit     │
│   Service   │
└─────────────┘
```

### Flux de Données

1. **Authentification** : Client → Keycloak → Token JWT
2. **Création Compte** : Client → Account Service (avec token)
3. **Mapping** : Crypto Service → Account Service (GET /api/accounts/user/{userId})
4. **Transaction** : Crypto Service → Account Service (débit/crédit)
5. **Audit** : Crypto Service → Audit Service (événements)

### Points Clés

✅ **Authentification centralisée** via Keycloak
✅ **Service Discovery** via Eureka
✅ **Communication inter-services** via Feign Client
✅ **Propagation du token JWT** entre services
✅ **Mapping userId -> accountRef** via endpoint dédié
✅ **Audit logging** automatique

## 🎯 Prochaines Étapes

1. Exécutez `.\test-complete-integration.ps1`
2. Vérifiez tous les résultats
3. Consultez les logs des services pour confirmer
4. Testez avec différents utilisateurs et rôles
5. Testez les cas d'erreur (solde insuffisant, service indisponible, etc.)
