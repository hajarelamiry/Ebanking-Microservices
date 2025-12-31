# Guide de Test Postman - E-Banking Microservices

Ce guide contient toutes les requêtes pour tester les APIs des services Payment, Crypto et Audit.

## 📋 Table des matières

1. [Configuration Postman](#configuration-postman)
2. [Payment Service](#payment-service)
3. [Crypto Service](#crypto-service)
4. [Audit Service](#audit-service)
5. [Eureka Server](#eureka-server)

---

## 🔧 Configuration Postman

### Variables d'environnement (optionnel mais recommandé)

Créez un environnement Postman avec ces variables :

```
base_url_payment: http://localhost:8080
base_url_crypto: http://localhost:8082
base_url_audit: http://localhost:8083
base_url_eureka: http://localhost:8761
```

---

## 💳 Payment Service

**Base URL:** `http://localhost:8080`

### 1. Créer un virement standard (succès)

**Method:** `POST`  
**URL:** `http://localhost:8080/api/v1/payments`  
**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "sourceAccountId": "ACC123456",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 500.0,
  "type": "STANDARD"
}
```

**Réponse attendue (201 Created):**
```json
{
  "id": 1,
  "sourceAccountId": "ACC123456",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 500.0,
  "status": "VALIDATED",
  "message": "Transaction créée avec succès",
  "createdAt": "2025-01-XX..."
}
```

---

### 2. Créer un virement instantané

**Method:** `POST`  
**URL:** `http://localhost:8080/api/v1/payments`  
**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "sourceAccountId": "ACC123456",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 200.0,
  "type": "INSTANT"
}
```

---

### 3. Créer un virement rejeté (montant > 10000€)

**Method:** `POST`  
**URL:** `http://localhost:8080/api/v1/payments`  
**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "sourceAccountId": "ACC123456",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 15000.0,
  "type": "STANDARD"
}
```

**Réponse attendue (422 Unprocessable Entity):**
```json
{
  "id": 2,
  "sourceAccountId": "ACC123456",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 15000.0,
  "status": "REJECTED",
  "message": "Transaction rejetée: montant supérieur au seuil autorisé (15000.00€ > 10000.00€)",
  "createdAt": "2025-01-XX..."
}
```

---

### 4. Accéder à Swagger UI

**URL:** `http://localhost:8080/swagger-ui.html`

---

## 🪙 Crypto Service

**Base URL:** `http://localhost:8082`

### 1. Récupérer les prix des cryptomonnaies

**Method:** `GET`  
**URL:** `http://localhost:8082/api/v1/crypto/prices`

**Réponse attendue (200 OK):**
```json
{
  "prices": {
    "BTC": 45000.0,
    "ETH": 3000.0,
    "BNB": 400.0,
    "ADA": 0.5,
    "SOL": 100.0
  }
}
```

---

### 2. Récupérer le wallet d'un utilisateur

**Method:** `GET`  
**URL:** `http://localhost:8082/api/v1/crypto/wallet?userId=1`

**Réponse attendue (200 OK):**
```json
{
  "userId": 1,
  "wallets": [
    {
      "symbol": "BTC",
      "balance": 0.5
    },
    {
      "symbol": "ETH",
      "balance": 2.0
    }
  ]
}
```

---

### 3. Créer un trade BUY (achat)

**Method:** `POST`  
**URL:** `http://localhost:8082/api/v1/crypto/trade?userId=1`  
**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "symbol": "BTC",
  "quantity": 0.1,
  "type": "BUY"
}
```

**Réponse attendue (201 Created):**
```json
{
  "id": 1,
  "userId": 1,
  "symbol": "BTC",
  "type": "BUY",
  "quantity": 0.1,
  "priceAtTime": 45000.0,
  "timestamp": "2025-01-XX..."
}
```

---

### 4. Créer un trade SELL (vente)

**Method:** `POST`  
**URL:** `http://localhost:8082/api/v1/crypto/trade?userId=1`  
**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "symbol": "ETH",
  "quantity": 0.5,
  "type": "SELL"
}
```

**Note:** La vente échouera si le solde est insuffisant (erreur 500).

---

### 5. Récupérer l'historique des transactions

**Method:** `GET`  
**URL:** `http://localhost:8082/api/v1/crypto/history?userId=1`

**Réponse attendue (200 OK):**
```json
[
  {
    "id": 2,
    "userId": 1,
    "symbol": "ETH",
    "type": "SELL",
    "quantity": 0.5,
    "priceAtTime": 3000.0,
    "timestamp": "2025-01-XX..."
  },
  {
    "id": 1,
    "userId": 1,
    "symbol": "BTC",
    "type": "BUY",
    "quantity": 0.1,
    "priceAtTime": 45000.0,
    "timestamp": "2025-01-XX..."
  }
]
```

---

## 📊 Audit Service

**Base URL:** `http://localhost:8083`

### 1. Health Check

**Method:** `GET`  
**URL:** `http://localhost:8083/api/audit/health`

**Réponse attendue (200 OK):**
```json
{
  "status": "UP",
  "service": "audit-service"
}
```

---

### 2. Journaliser un événement

**Method:** `POST`  
**URL:** `http://localhost:8083/api/audit/events`  
**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "userId": "test-user-001",
  "actionType": "LOGIN",
  "serviceName": "auth-service",
  "description": "Connexion utilisateur",
  "status": "SUCCESS"
}
```

**Réponse attendue (201 Created):**
```json
{
  "message": "Audit event logged successfully",
  "auditLogId": 1,
  "timestamp": "2025-01-XX..."
}
```

---

### 3. Recevoir un événement externe

**Method:** `POST`  
**URL:** `http://localhost:8083/api/audit/events/external`  
**Headers:**
```
Content-Type: application/json
```

**Body (raw JSON):**
```json
{
  "userId": "test-user-002",
  "actionType": "VIREMENT",
  "serviceName": "payment-service",
  "description": "Virement externe",
  "status": "SUCCESS"
}
```

**Réponse attendue (201 Created):**
```json
{
  "message": "External audit event received and logged",
  "auditLogId": 2,
  "timestamp": "2025-01-XX..."
}
```

---

### 4. Historique par utilisateur

**Method:** `GET`  
**URL:** `http://localhost:8083/api/audit/users/test-user-001/history`

**Paramètres optionnels:**
- `page`: numéro de page (défaut: 0)
- `size`: taille de la page (défaut: 20)
- `actionType`: filtrer par type d'action
- `status`: filtrer par statut (SUCCESS, FAILURE, ERROR)
- `startDate`: date de début (format ISO: 2025-01-01T00:00:00)
- `endDate`: date de fin (format ISO: 2025-01-31T23:59:59)

**Exemple avec pagination:**
```
http://localhost:8083/api/audit/users/test-user-001/history?page=0&size=10
```

**Exemple avec filtres:**
```
http://localhost:8083/api/audit/users/test-user-001/history?actionType=LOGIN&status=SUCCESS
```

**Réponse attendue (200 OK):**
```json
{
  "userId": "test-user-001",
  "totalElements": 5,
  "totalPages": 1,
  "currentPage": 0,
  "auditLogs": [
    {
      "id": 5,
      "userId": "test-user-001",
      "actionType": "LOGIN",
      "serviceName": "auth-service",
      "description": "Connexion utilisateur",
      "status": "SUCCESS",
      "timestamp": "2025-01-XX..."
    }
  ]
}
```

---

### 5. Historique global (admin)

**Method:** `GET`  
**URL:** `http://localhost:8083/api/audit/history`

**Paramètres optionnels:**
- `page`: numéro de page (défaut: 0)
- `size`: taille de la page (défaut: 20)
- `userId`: filtrer par utilisateur
- `actionType`: filtrer par type d'action
- `serviceName`: filtrer par service (payment-service, crypto-service, etc.)
- `status`: filtrer par statut
- `startDate`: date de début
- `endDate`: date de fin

**Exemples:**

1. **Sans filtres:**
```
http://localhost:8083/api/audit/history?page=0&size=10
```

2. **Filtrer par service:**
```
http://localhost:8083/api/audit/history?serviceName=payment-service&size=10
```

3. **Filtrer par service et statut:**
```
http://localhost:8083/api/audit/history?serviceName=payment-service&status=SUCCESS&size=5
```

4. **Filtrer par service crypto:**
```
http://localhost:8083/api/audit/history?serviceName=crypto-service&size=10
```

**Réponse attendue (200 OK):**
```json
{
  "totalElements": 10,
  "totalPages": 1,
  "currentPage": 0,
  "auditLogs": [
    {
      "id": 10,
      "userId": "ACC123456",
      "actionType": "PAYMENT_CREATED",
      "serviceName": "payment-service",
      "description": "Payment transaction validated",
      "status": "SUCCESS",
      "timestamp": "2025-01-XX..."
    }
  ]
}
```

---

### 6. Récupérer les erreurs

**Method:** `GET`  
**URL:** `http://localhost:8083/api/audit/errors`

**Paramètres optionnels:**
- `page`: numéro de page (défaut: 0)
- `size`: taille de la page (défaut: 20)

**Exemple:**
```
http://localhost:8083/api/audit/errors?page=0&size=10
```

**Réponse attendue (200 OK):**
```json
{
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "auditLogs": [
    {
      "id": 3,
      "userId": "ACC123456",
      "actionType": "PAYMENT_REJECTED",
      "serviceName": "payment-service",
      "description": "Payment transaction rejected",
      "status": "FAILURE",
      "errorMessage": "Transaction rejetée: montant supérieur au seuil autorisé",
      "timestamp": "2025-01-XX..."
    }
  ]
}
```

---

### 7. Statistiques par utilisateur

**Method:** `GET`  
**URL:** `http://localhost:8083/api/audit/stats/user/test-user-001`

**Réponse attendue (200 OK):**
```json
{
  "userId": "test-user-001",
  "totalActions": 5
}
```

---

### 8. Statistiques des erreurs

**Method:** `GET`  
**URL:** `http://localhost:8083/api/audit/stats/errors`

**Réponse attendue (200 OK):**
```json
{
  "totalErrors": 2
}
```

---

## 🔍 Eureka Server

**Base URL:** `http://localhost:8761`

### 1. Accéder au dashboard Eureka

**Method:** `GET`  
**URL:** `http://localhost:8761`

Ouvrez cette URL dans votre navigateur pour voir l'interface web d'Eureka.

---

### 2. Liste des applications (JSON)

**Method:** `GET`  
**URL:** `http://localhost:8761/eureka/apps`  
**Headers:**
```
Accept: application/json
```

**Réponse attendue (200 OK):**
```json
{
  "applications": {
    "versions__delta": "1",
    "apps__hashcode": "UP_3_",
    "application": [
      {
        "name": "PAYMENT-SERVICE",
        "instance": [
          {
            "instanceId": "payment-service:...",
            "hostName": "...",
            "app": "PAYMENT-SERVICE",
            "ipAddr": "127.0.0.1",
            "status": "UP",
            "port": {
              "$": 8080,
              "@enabled": "true"
            }
          }
        ]
      },
      {
        "name": "CRYPTO-SERVICE",
        "instance": [...]
      },
      {
        "name": "AUDIT-SERVICE",
        "instance": [...]
      }
    ]
  }
}
```

---

## 🧪 Scénarios de test recommandés

### Scénario 1: Test complet d'un virement avec audit

1. **Créer un virement** (Payment Service)
   - POST `/api/v1/payments` avec montant < 10000€
   - Vérifier le statut `VALIDATED`

2. **Vérifier l'événement dans Audit** (Attendre 5 secondes)
   - GET `/api/audit/history?serviceName=payment-service&size=10`
   - Vérifier qu'un événement `PAYMENT_CREATED` avec statut `SUCCESS` est présent

---

### Scénario 2: Test d'un trade crypto avec audit

1. **Récupérer les prix** (Crypto Service)
   - GET `/api/v1/crypto/prices`

2. **Créer un trade BUY** (Crypto Service)
   - POST `/api/v1/crypto/trade?userId=1` avec type `BUY`

3. **Vérifier l'événement dans Audit** (Attendre 5 secondes)
   - GET `/api/audit/history?serviceName=crypto-service&size=10`
   - Vérifier qu'un événement `CRYPTO_BUY` avec statut `SUCCESS` est présent

---

### Scénario 3: Test des filtres d'audit

1. **Créer plusieurs événements** via Payment et Crypto Services

2. **Tester les filtres:**
   - GET `/api/audit/history?serviceName=payment-service` → Voir uniquement les événements Payment
   - GET `/api/audit/history?serviceName=crypto-service` → Voir uniquement les événements Crypto
   - GET `/api/audit/history?serviceName=payment-service&status=SUCCESS` → Voir uniquement les succès Payment
   - GET `/api/audit/errors` → Voir uniquement les erreurs

---

## 📝 Notes importantes

1. **Attente pour les événements:** Les événements sont publiés via Kafka avec un délai (pattern Outbox). Attendez 5-10 secondes après une opération avant de vérifier dans Audit.

2. **Codes de statut HTTP:**
   - `200 OK`: Succès
   - `201 Created`: Ressource créée
   - `422 Unprocessable Entity`: Transaction rejetée (règles anti-fraude)
   - `500 Internal Server Error`: Erreur serveur

3. **Format des dates:** Utilisez le format ISO 8601 pour les dates:
   ```
   2025-01-15T10:30:00
   ```

4. **Pagination:** Par défaut, les réponses sont paginées avec `page=0` et `size=20`.

---

## 🚀 Import dans Postman

Vous pouvez créer une collection Postman avec toutes ces requêtes. Voici un exemple de structure:

```
E-Banking Microservices
├── Payment Service
│   ├── Create Payment (Standard)
│   ├── Create Payment (Instant)
│   └── Create Payment (Rejected)
├── Crypto Service
│   ├── Get Prices
│   ├── Get Wallet
│   ├── Trade BUY
│   ├── Trade SELL
│   └── Get History
├── Audit Service
│   ├── Health Check
│   ├── Log Event
│   ├── Get User History
│   ├── Get Global History
│   ├── Get Errors
│   └── Get Stats
└── Eureka Server
    └── Get Applications
```

---

## ✅ Checklist de test

- [ ] Payment Service: Créer un virement standard
- [ ] Payment Service: Créer un virement rejeté (montant > 10000€)
- [ ] Crypto Service: Récupérer les prix
- [ ] Crypto Service: Créer un trade BUY
- [ ] Audit Service: Vérifier les événements Payment (après 5-10 secondes)
- [ ] Audit Service: Vérifier les événements Crypto (après 5-10 secondes)
- [ ] Audit Service: Tester les filtres (serviceName, status)
- [ ] Eureka: Vérifier que tous les services sont enregistrés

---

**Bon test ! 🎉**
