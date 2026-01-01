# Guide de Test - Mapping userId -> accountRef

Ce guide explique comment tester le nouveau mapping `userId -> accountRef` et les transactions crypto.

## Prérequis

1. **Services démarrés** :
   - ✅ Keycloak sur le port **8180**
   - ✅ Eureka Server sur le port **8761**
   - ✅ account-service sur le port **8087**
   - ✅ crypto-service sur le port **8085**
   - ✅ payment-service sur le port **8086** (optionnel pour ces tests)

2. **Configuration Keycloak** :
   - Realm : `ebanking-realm`
   - Client : `ebanking-client`
   - Client Secret : `your-client-secret` (à adapter selon votre configuration)
   - Utilisateur de test avec rôle **CLIENT** : `client1` / `password` (à adapter)

3. **Compte utilisateur** :
   - L'utilisateur de test doit avoir un compte créé dans `account-service`
   - Le compte doit être en devise **EUR** (ou être le premier compte actif)

## Tests à Exécuter

### Test 1 : Endpoint GET /api/accounts/user/{userId}

**Script** : `test-account-by-userid.ps1`

**Objectif** : Vérifier que l'endpoint retourne correctement l'accountRef d'un utilisateur.

**Étapes** :
1. Modifier les identifiants dans le script (lignes 79-80) :
   ```powershell
   $testUsername = "client1"  # Votre utilisateur de test
   $testPassword = "password"  # Son mot de passe
   ```

2. Exécuter le script :
   ```powershell
   .\test-account-by-userid.ps1
   ```

3. **Résultat attendu** :
   - ✅ Token obtenu avec succès
   - ✅ Account Reference récupéré
   - ✅ Balance et devise affichés

**Si le test échoue** :
- Vérifiez que Keycloak est démarré sur le port 8180
- Vérifiez que account-service est démarré sur le port 8087
- Vérifiez que l'utilisateur a un compte créé dans account-service
- Créez un compte si nécessaire :
  ```powershell
  POST http://localhost:8087/api/accounts
  Authorization: Bearer {token}
  Body: {
    "devise": "EUR",
    "initialBalance": 1000.00
  }
  ```

### Test 2 : Transactions Crypto avec Mapping

**Script** : `test-crypto-transactions.ps1`

**Objectif** : Vérifier que les transactions crypto utilisent correctement le mapping `userId -> accountRef`.

**Étapes** :
1. Modifier les identifiants dans le script (lignes 48-49) :
   ```powershell
   $testUsername = "client1"  # Votre utilisateur de test
   $testPassword = "password"  # Son mot de passe
   ```

2. Exécuter le script :
   ```powershell
   .\test-crypto-transactions.ps1
   ```

3. **Résultat attendu** :
   - ✅ Token obtenu avec succès
   - ✅ Compte utilisateur trouvé (via GET /api/accounts/user/{userId})
   - ✅ Prix crypto récupérés
   - ✅ Achat avec solde insuffisant rejeté
   - ✅ Achat avec solde suffisant effectué
   - ✅ Compte débité correctement
   - ✅ Wallet crypto crédité

**Si le test échoue** :
- Vérifiez que tous les services sont démarrés
- Vérifiez que l'utilisateur a un compte avec un solde suffisant
- Vérifiez les logs de crypto-service pour voir si l'appel à account-service fonctionne
- Vérifiez les logs de account-service pour voir si l'endpoint GET /api/accounts/user/{userId} est appelé

## Vérification Manuelle

### 1. Vérifier le mapping dans les logs

**Dans crypto-service**, lors d'une transaction, vous devriez voir :
```
Account reference retrieved for user client1: {accountRef}
```

**Dans account-service**, vous devriez voir :
```
GET /api/accounts/user/client1
```

### 2. Vérifier que l'accountRef est correct

1. Récupérer l'accountRef via l'endpoint :
   ```powershell
   GET http://localhost:8087/api/accounts/user/client1
   Authorization: Bearer {token}
   ```

2. Vérifier le solde du compte :
   ```powershell
   GET http://localhost:8087/api/accounts/{accountRef}/balance
   Authorization: Bearer {token}
   ```

3. Effectuer une transaction crypto :
   ```powershell
   POST http://localhost:8085/api/v1/crypto/trade
   Authorization: Bearer {token}
   Body: {
     "symbol": "BTC",
     "quantity": 0.001,
     "type": "BUY"
   }
   ```

4. Vérifier que le compte a été débité :
   ```powershell
   GET http://localhost:8087/api/accounts/{accountRef}/balance
   Authorization: Bearer {token}
   ```

## Points à Vérifier

✅ **Le mapping fonctionne si** :
- L'endpoint `GET /api/accounts/user/{userId}` retourne un accountRef
- Les transactions crypto utilisent cet accountRef (pas `userId.toString()`)
- Le compte est correctement débité/crédité lors des transactions
- Aucune erreur "Compte introuvable" n'est générée

❌ **Le mapping ne fonctionne pas si** :
- Erreur "Compte introuvable" lors des transactions crypto
- Le compte n'est pas débité/crédité
- Erreur 404 sur l'endpoint `GET /api/accounts/user/{userId}`
- Les logs montrent encore l'utilisation de `userId.toString()`

## Dépannage

### Erreur : "Aucun compte trouvé pour l'utilisateur"

**Solution** : Créez un compte pour l'utilisateur :
```powershell
POST http://localhost:8087/api/accounts
Authorization: Bearer {token}
Body: {
  "devise": "EUR",
  "initialBalance": 1000.00
}
```

### Erreur : "Account Service unavailable"

**Solution** :
- Vérifiez que account-service est démarré
- Vérifiez que Eureka peut résoudre le service
- Vérifiez la configuration Resilience4j dans crypto-service

### Erreur : "401 Unauthorized"

**Solution** :
- Vérifiez que le token JWT est valide
- Vérifiez que l'utilisateur a le rôle CLIENT
- Vérifiez la configuration Keycloak

### Erreur : "403 Forbidden" sur GET /api/accounts/user/{userId}

**Solution** :
- Vérifiez que l'utilisateur demande son propre compte
- L'endpoint vérifie que `authenticatedUsername.equals(userId)`

## Résumé

Le mapping `userId -> accountRef` est maintenant implémenté via :
1. **Endpoint** : `GET /api/accounts/user/{userId}` dans account-service
2. **Utilisation** : `accountService.getAccountRefByUserId(userId)` dans crypto-service
3. **Avantages** :
   - ✅ Aucune supposition sur le format de l'accountRef
   - ✅ Gestion propre des comptes multiples
   - ✅ Sécurité : vérification de propriété du compte
   - ✅ Maintenabilité : changement centralisé
