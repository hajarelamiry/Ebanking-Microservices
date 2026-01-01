# Problème Étape 6 - Erreur 400 "Demande Incorrecte"

## 🔍 Diagnostic

L'erreur 400 dans l'étape 6 (test transaction crypto avec solde suffisant) est causée par :

1. **`JwtUtils.getUserIdAsLong()` retourne `null`**
   - Le JWT contient `"user1"` (string) comme `preferred_username`
   - Le code essaie de parser `"user1"` en `Long`, ce qui échoue
   - Il essaie alors d'appeler `userService.getUserIdFromUserService()`

2. **`user-service` retourne 401**
   - Même après correction du port Keycloak (8180 → 8080)
   - Le service n'accepte pas le token JWT
   - Donc `getUserIdFromUserService()` retourne `null`

3. **Le contrôleur retourne 400**
   - Si `targetUserId` est `null`, le contrôleur retourne `BAD_REQUEST` (400)

## ✅ Corrections Appliquées

### 1. Configuration Keycloak
- ✅ `user-service` : Port Keycloak corrigé (8180 → 8080)
- ✅ `account-service` : Port Keycloak corrigé (8180 → 8080)

### 2. Gestion d'Erreur Améliorée
- ✅ `CryptoController.trade()` : Meilleure gestion quand `getUserIdAsLong()` retourne `null`
- ✅ Tentative de récupération depuis `user-service` avec gestion d'erreur

### 3. Compilation
- ✅ `CryptoController` : Méthode `init()` dupliquée supprimée
- ✅ Compilation réussie

## 🔧 Actions Requises

### 1. Redémarrer les Services

**IMPORTANT** : Redémarrez les services suivants pour appliquer les changements :

```bash
# Redémarrer user-service (port 8082)
# Redémarrer account-service (port 8087)
# Redémarrer crypto-service (port 8085)
```

### 2. Vérifier user-service

Après redémarrage, testez manuellement :

```powershell
# Obtenir un token
$tokenUrl = "http://localhost:8080/realms/ebanking-realm/protocol/openid-connect/token"
$body = @{
    grant_type = "password"
    client_id = "ebanking-client"
    username = "user1"
    password = "password"
}
$tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
$token = $tokenResponse.access_token

# Tester user-service
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}
Invoke-RestMethod -Uri "http://localhost:8082/api/customers/me" -Method Get -Headers $headers
```

**Résultat attendu** :
```json
{
  "id": 1,
  "username": "user1",
  "email": "user1@ebanking.com",
  "firstName": "User",
  "lastName": "One",
  "kycStatus": "VERIFIED"
}
```

### 3. Relancer le Test

Une fois que `user-service` fonctionne :

```powershell
.\test-integration-simple.ps1
```

## 📋 Vérifications

### Si user-service retourne toujours 401

1. **Vérifier que user-service est redémarré**
2. **Vérifier la configuration Keycloak dans `application.yml`** :
   ```yaml
   spring:
     security:
       oauth2:
         resourceserver:
           jwt:
             issuer-uri: http://localhost:8080/realms/ebanking-realm
             jwk-set-uri: http://localhost:8080/realms/ebanking-realm/protocol/openid-connect/certs
   ```

3. **Vérifier que Keycloak est accessible** :
   ```powershell
   Invoke-WebRequest -Uri "http://localhost:8080/realms/ebanking-realm" -Method GET
   ```

4. **Vérifier les logs de user-service** pour voir l'erreur exacte

### Si l'erreur 400 persiste après correction de user-service

Le problème peut venir de :
1. **Le compte n'existe pas** dans account-service
2. **Le mapping userId → accountRef échoue**
3. **Le format de la requête** est incorrect

## 🎯 Solution Alternative

Si `user-service` n'est toujours pas accessible, vous pouvez passer le `userId` en paramètre dans la requête :

```powershell
# Récupérer l'ID utilisateur depuis user-service d'abord
$userInfo = Invoke-RestMethod -Uri "http://localhost:8082/api/customers/me" -Method Get -Headers $headers
$userId = $userInfo.id

# Passer userId en paramètre
$tradeUrl = "http://localhost:8085/api/v1/crypto/trade?userId=$userId"
$tradeBody = @{
    symbol = "BTC"
    quantity = 0.001
    type = "BUY"
} | ConvertTo-Json

Invoke-RestMethod -Uri $tradeUrl -Method Post -Headers $headers -Body $tradeBody
```

## 📝 Résumé

- ✅ **Compilation** : Réussie
- ✅ **Configuration Keycloak** : Corrigée (ports 8080)
- ✅ **Gestion d'erreur** : Améliorée
- ⚠️ **Action requise** : Redémarrer user-service, account-service et crypto-service
- ⚠️ **Vérification** : Tester que user-service accepte les tokens JWT

Une fois les services redémarrés, l'erreur 400 devrait être résolue.
