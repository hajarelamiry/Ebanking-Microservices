# Guide du Mode Mock - Payment Service

## 🎯 Objectif

Permettre au **payment-service** de fonctionner à 100% de manière isolée, sans dépendre des services externes (`account-service` et `legacy-adapter-service`).

---

## 🚀 Activation du Mode Mock

### Option 1 : Via les propriétés (Recommandé)

Dans `application.properties`, ajoutez :

```properties
spring.profiles.active=mock
payment.mock.enabled=true
```

### Option 2 : Via les variables d'environnement

```bash
export SPRING_PROFILES_ACTIVE=mock
export PAYMENT_MOCK_ENABLED=true
```

### Option 3 : Via la ligne de commande

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=mock
```

---

## ✅ Ce qui est simulé

### 1. **AccountServiceClient (Mock)**

Simule les opérations sur les comptes :

- ✅ **getAccountBalance()** : Retourne toujours un solde de 10 000 EUR
- ✅ **debitAccount()** : Simule le débit (solde - montant)
- ✅ **creditAccount()** : Simule le crédit (solde + montant)
- ✅ **checkSufficientBalance()** : Retourne toujours `true`

**Logs** : Tous les appels sont préfixés avec `🔵 [MOCK]`

### 2. **LegacyAdapterClient (Mock)**

Simule les appels SOAP au système legacy :

- ✅ **sendPayment()** : 
  - 95% de succès (simulation normale)
  - 5% d'échec (pour tester la compensation Saga)
  - Génère une référence legacy fictive

**Logs** : Tous les appels sont préfixés avec `🟡 [MOCK]`

---

## 📋 Fonctionnalités Testables en Mode Mock

### ✅ Virement STANDARD
- Création de transaction
- Détection de fraude
- Envoi simulé au legacy
- Enregistrement en base de données

### ✅ Virement INSTANT
- Vérification du solde (simulée)
- Débit immédiat (simulé)
- Envoi simulé au legacy
- Compensation en cas d'échec (testable)

### ✅ Détection de Fraude
- IBAN blacklisté
- Plafonds journaliers/mensuels
- Vélocité des transactions

### ✅ Pattern Saga
- Orchestration des transactions
- Compensation automatique
- Événements Kafka (si Kafka est disponible)

---

## 🔍 Vérification que le Mode Mock est Actif

Au démarrage, vous verrez dans les logs :

```
═══════════════════════════════════════════════════════════
🔵 MODE MOCK ACTIVÉ - Services externes simulés
🔵 AccountServiceClient: MOCK
🔵 LegacyAdapterClient: MOCK
═══════════════════════════════════════════════════════════
```

Pendant les appels, vous verrez :

```
🔵 [MOCK] Vérification du solde pour le compte: 550e8400-...
🔵 [MOCK] Solde simulé: 10000.00 EUR
🟡 [MOCK] Envoi simulé au Legacy Adapter pour la transaction: ...
```

---

## 🧪 Tests avec Postman

Tous les scénarios de test fonctionnent en mode mock :

### Test 1 : Virement Standard
```json
POST http://localhost:8080/api/payments/transfer

{
  "sourceAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 100.50,
  "currency": "EUR",
  "type": "STANDARD"
}
```

**Résultat attendu** : Transaction créée, envoyée au legacy (simulé), statut VALIDATED

### Test 2 : Virement Instantané
```json
{
  "sourceAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 250.75,
  "currency": "EUR",
  "type": "INSTANT"
}
```

**Résultat attendu** : 
- Solde vérifié (simulé) ✅
- Compte débité (simulé) ✅
- Envoyé au legacy (simulé) ✅
- Statut VALIDATED ✅

---

## 🔄 Passage en Mode Production

Quand les services réels seront disponibles :

### 1. Désactiver le mode mock

Dans `application.properties` :

```properties
# Commenter ou supprimer:
# spring.profiles.active=mock
# payment.mock.enabled=true

# OU explicitement:
spring.profiles.active=prod
payment.mock.enabled=false
```

### 2. Configurer les URLs réelles

```properties
feign.client.account-service.url=http://account-service:8081
feign.client.legacy-adapter-service.url=http://legacy-adapter-service:8082
```

### 3. Vérifier les services

Les clients Feign réels seront utilisés automatiquement.

---

## 📊 Architecture en Mode Mock

```
┌─────────────────────────────────────────┐
│      PaymentController (REST)           │
└──────────────┬──────────────────────────┘
               │
               ▼
┌─────────────────────────────────────────┐
│      PaymentService                     │
│  - Détection de fraude ✅               │
│  - Logique métier ✅                    │
└──────────────┬──────────────────────────┘
               │
       ┌───────┴───────┐
       ▼               ▼
┌──────────────┐  ┌──────────────┐
│ MockAccount  │  │ MockLegacy   │
│ ServiceClient│  │ AdapterClient│
│   🔵 MOCK    │  │   🟡 MOCK    │
└──────────────┘  └──────────────┘
```

---

## ⚠️ Limitations du Mode Mock

1. **Solde fixe** : Tous les comptes ont le même solde simulé (10 000 EUR)
2. **Pas de validation réelle** : Les vérifications de solde sont toujours positives
3. **Legacy simulé** : Les appels SOAP ne sont pas réels
4. **Pas de service discovery** : Les URLs sont fixes

**Ces limitations sont acceptables pour le développement isolé.**

---

## 🎯 Avantages

✅ **Développement isolé** : Pas besoin des autres services  
✅ **Tests rapides** : Pas de dépendances externes  
✅ **Focus métier** : Concentrez-vous sur la logique de virement  
✅ **Intégration progressive** : Passez en mode réel quand prêt  
✅ **Documentation claire** : Le code montre ce qui est mocké  

---

## 📝 Notes Importantes

- Les mocks sont **automatiquement désactivés** si `payment.mock.enabled=false`
- Le mode mock utilise `@Primary` pour remplacer les clients Feign
- Les logs sont clairement marqués avec `🔵 [MOCK]` et `🟡 [MOCK]`
- La base de données PostgreSQL est toujours utilisée (pas mockée)
- Kafka peut être mocké aussi si nécessaire

---

## 🚀 Prochaines Étapes

1. ✅ Activer le mode mock
2. ✅ Tester tous les scénarios avec Postman
3. ✅ Vérifier les logs de simulation
4. ✅ Valider la détection de fraude
5. ✅ Tester la compensation Saga
6. 🔄 Quand prêt : Désactiver les mocks et intégrer les vrais services

