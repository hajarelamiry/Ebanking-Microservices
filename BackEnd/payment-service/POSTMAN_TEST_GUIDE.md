# Guide de Test Postman - Payment Service

## 📋 Configuration de Base

### URL de Base
```
http://localhost:8080
```

**Note** : Vérifiez le port dans les logs au démarrage (par défaut Spring Boot utilise le port 8080)

### Headers Requis
```
Content-Type: application/json
Accept: application/json
```

---

## 🚀 Endpoints Disponibles

### 1. Créer un Virement
**POST** `/api/payments/transfer`

---

## 📝 Scénarios de Test

### ✅ Test 1 : Virement Standard (STANDARD)

**Request:**
```http
POST http://localhost:8080/api/payments/transfer
Content-Type: application/json

{
  "sourceAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 100.50,
  "currency": "EUR",
  "type": "STANDARD"
}
```

**Response attendue (202 Accepted):**
```json
{
  "transactionId": "123e4567-e89b-12d3-a456-426614174000",
  "status": "VALIDATED",
  "message": "Virement standard enregistré et en cours de traitement",
  "createdAt": "2025-12-29T20:30:00"
}
```

---

### ⚡ Test 2 : Virement Instantané (INSTANT)

**Request:**
```http
POST http://localhost:8080/api/payments/transfer
Content-Type: application/json

{
  "sourceAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 250.75,
  "currency": "EUR",
  "type": "INSTANT"
}
```

**Response attendue (202 Accepted):**
```json
{
  "transactionId": "123e4567-e89b-12d3-a456-426614174001",
  "status": "VALIDATED",
  "message": "Virement instantané validé et en cours de traitement",
  "createdAt": "2025-12-29T20:30:00"
}
```

**Note** : Pour un virement INSTANT, le service vérifie le solde et débite immédiatement le compte.

---

### 🚨 Test 3 : Détection de Fraude - IBAN Blacklisté

**Prérequis** : Ajouter un IBAN dans la liste noire via la base de données

**Request:**
```http
POST http://localhost:8080/api/payments/transfer
Content-Type: application/json

{
  "sourceAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationIban": "FR1420041010050500013M02699",
  "amount": 500.00,
  "currency": "EUR",
  "type": "STANDARD"
}
```

**Response attendue (200 OK avec statut FRAUD_SUSPECTED):**
```json
{
  "transactionId": "123e4567-e89b-12d3-a456-426614174002",
  "status": "FRAUD_SUSPECTED",
  "message": "Transaction suspectée de fraude. Validation manuelle requise. Règle violée: IBAN_DESTINATION_BLACKLISTED",
  "createdAt": "2025-12-29T20:30:00"
}
```

---

### 🚨 Test 4 : Détection de Fraude - Plafond Journalier Dépassé

**Prérequis** : 
- Créer un `AccountLimit` avec un plafond journalier bas (ex: 100 EUR)
- Effectuer plusieurs transactions pour dépasser le plafond

**Request:**
```http
POST http://localhost:8080/api/payments/transfer
Content-Type: application/json

{
  "sourceAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 150.00,
  "currency": "EUR",
  "type": "STANDARD"
}
```

**Response attendue (200 OK avec statut FRAUD_SUSPECTED):**
```json
{
  "transactionId": "123e4567-e89b-12d3-a456-426614174003",
  "status": "FRAUD_SUSPECTED",
  "message": "Transaction suspectée de fraude. Validation manuelle requise. Règle violée: DAILY_LIMIT_EXCEEDED",
  "createdAt": "2025-12-29T20:30:00"
}
```

---

### 🚨 Test 5 : Détection de Fraude - Vélocité (Trop de Transactions)

**Prérequis** : Effectuer 5+ transactions en moins de 10 minutes

**Request (6ème transaction):**
```http
POST http://localhost:8080/api/payments/transfer
Content-Type: application/json

{
  "sourceAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 50.00,
  "currency": "EUR",
  "type": "STANDARD"
}
```

**Response attendue (200 OK avec statut FRAUD_SUSPECTED):**
```json
{
  "transactionId": "123e4567-e89b-12d3-a456-426614174004",
  "status": "FRAUD_SUSPECTED",
  "message": "Transaction suspectée de fraude. Validation manuelle requise. Règle violée: VELOCITY_THRESHOLD_EXCEEDED",
  "createdAt": "2025-12-29T20:30:00"
}
```

---

### ❌ Test 6 : Validation - Montant Invalide

**Request:**
```http
POST http://localhost:8080/api/payments/transfer
Content-Type: application/json

{
  "sourceAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": -10.00,
  "currency": "EUR",
  "type": "STANDARD"
}
```

**Response attendue (400 Bad Request):**
```json
{
  "timestamp": "2025-12-29T20:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Le montant doit être supérieur à 0",
  "path": "/api/payments/transfer"
}
```

---

### ❌ Test 7 : Validation - IBAN Invalide

**Request:**
```http
POST http://localhost:8080/api/payments/transfer
Content-Type: application/json

{
  "sourceAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationIban": "INVALID_IBAN",
  "amount": 100.00,
  "currency": "EUR",
  "type": "STANDARD"
}
```

**Response attendue (400 Bad Request):**
```json
{
  "timestamp": "2025-12-29T20:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "L'IBAN ne peut pas dépasser 34 caractères",
  "path": "/api/payments/transfer"
}
```

---

### ❌ Test 8 : Validation - Champs Manquants

**Request:**
```http
POST http://localhost:8080/api/payments/transfer
Content-Type: application/json

{
  "sourceAccountId": "550e8400-e29b-41d4-a716-446655440000",
  "destinationIban": "FR1420041010050500013M02606"
}
```

**Response attendue (400 Bad Request):**
```json
{
  "timestamp": "2025-12-29T20:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Le montant est requis",
  "path": "/api/payments/transfer"
}
```

---

## 🗄️ Préparation de la Base de Données pour les Tests

### 1. Ajouter un IBAN dans la Liste Noire

```sql
INSERT INTO blacklisted_ibans (id, iban, reason, is_active, created_at)
VALUES (
    gen_random_uuid(),
    'FR1420041010050500013M02699',
    'IBAN frauduleux détecté',
    true,
    NOW()
);
```

### 2. Créer un Plafond de Compte

```sql
INSERT INTO account_limits (id, account_id, daily_limit, monthly_limit, currency, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '550e8400-e29b-41d4-a716-446655440000'::uuid,
    100.00,
    5000.00,
    'EUR',
    NOW(),
    NOW()
);
```

### 3. Vérifier les Transactions

```sql
SELECT * FROM transactions ORDER BY created_at DESC LIMIT 10;
```

### 4. Vérifier les Vérifications de Fraude

```sql
SELECT fc.*, t.status, t.amount 
FROM fraud_checks fc
JOIN transactions t ON fc.transaction_id = t.id
ORDER BY fc.created_at DESC;
```

---

## 📊 Collection Postman

### Importer dans Postman

Créez une nouvelle collection "Payment Service" et ajoutez ces requêtes :

1. **Virement Standard**
2. **Virement Instantané**
3. **Test Fraude - IBAN Blacklisté**
4. **Test Fraude - Plafond Dépassé**
5. **Test Fraude - Vélocité**
6. **Test Validation - Montant Invalide**
7. **Test Validation - IBAN Invalide**
8. **Test Validation - Champs Manquants**

### Variables d'Environnement Postman

Créez un environnement avec ces variables :

```
base_url: http://localhost:8080
source_account_id: 550e8400-e29b-41d4-a716-446655440000
destination_iban: FR1420041010050500013M02606
```

---

## 🔍 Vérification des Logs

Pendant les tests, surveillez les logs pour voir :

1. **Détection de fraude** : `Transaction {} suspectée de fraude`
2. **Envoi au legacy** : `Envoi de la transaction {} au legacy adapter`
3. **Événements Kafka** : `Événement Saga publié`
4. **Compensation** : `Compensation de la transaction {}`

---

## ⚠️ Notes Importantes

1. **Services Externes** : Les appels à `account-service` et `legacy-adapter-service` échoueront si ces services ne sont pas démarrés. C'est normal pour les tests.

2. **Kafka** : Les événements Saga seront publiés même si Kafka n'est pas disponible (ils seront loggés).

3. **PostgreSQL** : Assurez-vous que PostgreSQL est démarré et que la base `ebanking_payment` existe.

4. **UUIDs** : Utilisez de vrais UUIDs pour `sourceAccountId` ou générez-les avec un outil en ligne.

---

## 🎯 Checklist de Test

- [ ] Virement STANDARD créé avec succès
- [ ] Virement INSTANT créé avec succès
- [ ] Fraude détectée pour IBAN blacklisté
- [ ] Fraude détectée pour plafond dépassé
- [ ] Fraude détectée pour vélocité excessive
- [ ] Validation fonctionne pour montant invalide
- [ ] Validation fonctionne pour IBAN invalide
- [ ] Validation fonctionne pour champs manquants
- [ ] Transactions enregistrées en base de données
- [ ] FraudChecks enregistrés en base de données

