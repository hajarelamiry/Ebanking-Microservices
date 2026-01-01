# Résumé des Tests - Mapping userId -> accountRef

## ✅ Implémentation Terminée

Le mapping `userId -> accountRef` a été correctement implémenté :

1. **Endpoint créé** : `GET /api/accounts/user/{userId}` dans account-service
2. **Feign Client mis à jour** : payment-service et crypto-service
3. **CryptoTradingService corrigé** : utilise maintenant `accountService.getAccountRefByUserId()`
4. **Compilation réussie** : tous les services compilent sans erreur

## 📋 Scripts de Test Créés

### 1. `test-account-by-userid.ps1`
Teste l'endpoint `GET /api/accounts/user/{userId}`

### 2. `test-crypto-transactions.ps1`
Teste les transactions crypto avec le nouveau mapping

### 3. `test-mapping-simple.ps1`
Script simplifié pour tester rapidement

### 4. `detect-services.ps1`
Détecte les ports actifs des services

## 🚀 Comment Tester (Une fois les services démarrés)

### Étape 1 : Vérifier que les services sont accessibles

```powershell
.\detect-services.ps1
```

### Étape 2 : Modifier la configuration dans les scripts

Dans `test-mapping-simple.ps1`, modifiez :
- `$keycloakUrl` : URL de Keycloak (défaut: http://localhost:8180)
- `$accountServiceUrl` : URL d'account-service (défaut: http://localhost:8087)
- `$cryptoServiceUrl` : URL de crypto-service (défaut: http://localhost:8085)
- `$testUsername` : Nom d'utilisateur de test (défaut: client1)
- `$testPassword` : Mot de passe de test (défaut: password)

### Étape 3 : Exécuter les tests

```powershell
# Test simple et rapide
.\test-mapping-simple.ps1

# Ou test complet
.\test-account-by-userid.ps1
.\test-crypto-transactions.ps1
```

## 🧪 Tests Manuels (Alternative)

### Test 1 : Endpoint GET /api/accounts/user/{userId}

1. **Obtenir un token Keycloak** :
```powershell
$tokenUrl = "http://localhost:8180/realms/ebanking-realm/protocol/openid-connect/token"
$body = @{
    grant_type = "password"
    client_id = "ebanking-client"
    client_secret = "your-client-secret"
    username = "client1"
    password = "password"
}
$response = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
$token = $response.access_token
```

2. **Tester l'endpoint** :
```powershell
$url = "http://localhost:8087/api/accounts/user/client1"
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}
$response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers
$response | ConvertTo-Json
```

**Résultat attendu** :
```json
{
  "externalReference": "uuid-du-compte",
  "balance": 1000.00,
  "devise": "EUR",
  "status": "ACTIF",
  "createdAt": "2024-01-01T00:00:00"
}
```

### Test 2 : Transaction Crypto

1. **Récupérer les prix** :
```powershell
$pricesUrl = "http://localhost:8085/api/v1/crypto/prices"
$pricesResponse = Invoke-RestMethod -Uri $pricesUrl -Method Get -Headers $headers
$btcPrice = $pricesResponse.prices.BTC
```

2. **Effectuer un achat** :
```powershell
$tradeUrl = "http://localhost:8085/api/v1/crypto/trade"
$tradeBody = @{
    symbol = "BTC"
    quantity = 0.001
    type = "BUY"
} | ConvertTo-Json

$tradeResponse = Invoke-RestMethod -Uri $tradeUrl -Method Post -Headers $headers -Body $tradeBody
$tradeResponse | ConvertTo-Json
```

3. **Vérifier que le compte a été débité** :
```powershell
$accountRef = $response.externalReference  # Du test 1
$balanceUrl = "http://localhost:8087/api/accounts/$accountRef/balance"
$balanceResponse = Invoke-RestMethod -Uri $balanceUrl -Method Get -Headers $headers
$balanceResponse | ConvertTo-Json
```

## ✅ Vérifications à Faire

### Dans les logs de crypto-service

Lors d'une transaction, vous devriez voir :
```
Account reference retrieved for user client1: {accountRef}
```

### Dans les logs d'account-service

Vous devriez voir :
```
GET /api/accounts/user/client1
```

### Vérification du code

Dans `CryptoTradingService.java`, ligne 43 :
```java
String accountRef = accountService.getAccountRefByUserId(userId.toString());
```

Cette ligne doit utiliser `getAccountRefByUserId()` et non `userId.toString()`.

## 🔍 Dépannage

### Erreur : "Aucun compte trouvé pour l'utilisateur"

**Solution** : Créez un compte pour l'utilisateur :
```powershell
$createAccountUrl = "http://localhost:8087/api/accounts"
$createBody = @{
    devise = "EUR"
    initialBalance = 1000.00
} | ConvertTo-Json

Invoke-RestMethod -Uri $createAccountUrl -Method Post -Headers $headers -Body $createBody
```

### Erreur : "Account Service unavailable"

**Vérifiez** :
- Que account-service est démarré
- Que Eureka peut résoudre le service
- Les logs de crypto-service pour voir l'erreur exacte

### Erreur : "401 Unauthorized"

**Vérifiez** :
- Que le token JWT est valide
- Que l'utilisateur a le rôle CLIENT
- La configuration Keycloak

## 📊 Résumé de l'Implémentation

### Fichiers Modifiés

**Account Service** :
- `service/AccountService.java` : Méthode `getAccountByUserId()` ajoutée
- `service/Impl/AccountServiceImpl.java` : Implémentation ajoutée
- `controller/AccountController.java` : Endpoint `GET /api/accounts/user/{userId}` ajouté

**Payment Service** :
- `client/AccountClient.java` : Méthode `getAccountByUserId()` ajoutée
- `client/AccountClientFallback.java` : Fallback ajouté
- `client/dto/AccountDto.java` : DTO créé

**Crypto Service** :
- `client/AccountClient.java` : Méthode `getAccountByUserId()` ajoutée
- `client/AccountClientFallback.java` : Fallback ajouté
- `client/dto/AccountDto.java` : DTO créé
- `service/AccountService.java` : Méthode `getAccountRefByUserId()` ajoutée
- `service/CryptoTradingService.java` : Utilise maintenant `getAccountRefByUserId()`

### Avantages

✅ **Aucune supposition sur le format** : L'accountRef est récupéré depuis account-service
✅ **Gestion des comptes multiples** : Priorité au compte EUR, sinon premier compte actif
✅ **Sécurité** : Vérification que l'utilisateur demande son propre compte
✅ **Maintenabilité** : Changement centralisé dans account-service

## 🎯 Prochaines Étapes

1. Démarrer tous les services
2. Exécuter `.\detect-services.ps1` pour vérifier les ports
3. Modifier la configuration dans les scripts si nécessaire
4. Exécuter `.\test-mapping-simple.ps1` pour tester
5. Vérifier les logs pour confirmer que le mapping fonctionne
