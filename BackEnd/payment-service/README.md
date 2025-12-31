# Payment Service

Microservice de gestion des virements bancaires pour le système E-Banking.

## 🏗️ Architecture

```
Controller → Service Interface → Service Impl → Repository
```

## 📦 Structure du Projet

```
src/main/java/com/example/payment_service/
├── enums/
│   ├── TransactionStatus.java    # PENDING, VALIDATED, REJECTED, COMPLETED
│   └── TransactionType.java      # STANDARD, INSTANT
├── model/
│   └── Payment.java              # Entité JPA
├── dto/
│   ├── PaymentRequestDTO.java    # DTO pour les requêtes
│   └── PaymentResponseDTO.java   # DTO pour les réponses
├── repository/
│   └── PaymentRepository.java    # Repository JPA
├── service/
│   ├── PaymentService.java          # Interface du service de paiement
│   ├── FraudDetectionService.java   # Interface du service de détection de fraude
│   └── impl/
│       ├── PaymentServiceImpl.java      # Implémentation du service de paiement
│       └── FraudDetectionServiceImpl.java # Implémentation du service de détection de fraude
├── controller/
│   └── PaymentController.java    # Contrôleur REST
└── config/
    └── OpenApiConfig.java         # Configuration Swagger/OpenAPI
```

## 🚀 Fonctionnalités

### 1. Création de Virement
- **Endpoint**: `POST /api/v1/payments`
- **Validation**: Vérification des champs obligatoires et contraintes
- **Règle Anti-Fraude**: Rejet automatique si montant > 10 000€

### 2. Règles Anti-Fraude

Le service implémente **3 règles anti-fraude** qui sont vérifiées dans l'ordre suivant :

#### Règle 1 : Montant > 10 000€ → REJECTED
- Seuil: **10 000€**
- Si montant > seuil → Statut `REJECTED`
- Message: "Transaction rejetée: montant supérieur au seuil autorisé"

#### Règle 2 : Vélocité (Plus de 3 virements en 10 minutes) → REJECTED
- Seuil: **3 virements maximum** dans une fenêtre de **10 minutes**
- Si le compte source a déjà effectué 3+ virements dans les 10 dernières minutes → Statut `REJECTED`
- Message: "Transaction rejetée: trop de virements récents"

#### Règle 3 : Nouveau bénéficiaire → PENDING_MANUAL_REVIEW
- Condition: IBAN jamais utilisé par ce compte source **ET** montant > 2 000€
- Si conditions remplies → Statut `PENDING_MANUAL_REVIEW` (validation manuelle requise)
- Message: "Transaction en attente de validation manuelle: nouveau bénéficiaire avec montant supérieur à 2000€"
- Si montant ≤ 2 000€ → Statut `PENDING` (traitement normal)

#### Règle 4 : Cumul journalier > 15 000€ → REJECTED
- Seuil: **15 000€** (total des virements du jour pour un compte source)
- Calcul: Somme de tous les virements effectués depuis le début de la journée + montant de la transaction actuelle
- Si cumul > seuil → Statut `REJECTED`
- Message: "Transaction rejetée: cumul journalier dépassé"
- **Vision globale**: Détecte les fraudes répétées avec de petits montants

#### Ordre de priorité
1. **REJECTED** (Règle 1, 2 ou 4) - Transaction bloquée définitivement
2. **PENDING_MANUAL_REVIEW** (Règle 3) - Nécessite validation humaine
3. **PENDING** → **VALIDATED** - Traitement automatique normal

### 3. Simulation Legacy Adapter
- Pour les transactions validées, simulation d'appel SOAP au `legacy-adapter-service`
- Mise à jour automatique du statut en `VALIDATED`

## 🛠️ Technologies

- **Java 17**
- **Spring Boot 3.2.5**
- **Spring Data JPA**
- **PostgreSQL**
- **Lombok**
- **Swagger/OpenAPI 3** (SpringDoc)

## 📋 Prérequis

- Java 17+
- Maven 3.6+
- PostgreSQL 12+ (optionnel pour les tests)

## 🏃 Démarrage

### 1. Configuration de la base de données

Modifiez `application.properties` si nécessaire:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ebanking_payment
spring.datasource.username=postgres
spring.datasource.password=root
```

### 2. Lancer l'application

```bash
mvn spring-boot:run
```

L'application démarre sur `http://localhost:8080`

### 3. Accéder à Swagger UI

```
http://localhost:8080/swagger-ui.html
```

## 🧪 Tests

### Exécuter tous les tests

```bash
mvn test
```

### Tests unitaires inclus

- `PaymentServiceImplTest`: Tests du service (règle anti-fraude, validation, etc.)
- `PaymentControllerTest`: Tests du contrôleur REST

## 📡 API Endpoints

### POST /api/v1/payments

Créer un nouveau virement.

**Request Body:**
```json
{
  "sourceAccountId": "ACC123456",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 500.0,
  "type": "STANDARD"
}
```

**Response 201 (Created):**
```json
{
  "id": 1,
  "sourceAccountId": "ACC123456",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 500.0,
  "status": "VALIDATED",
  "message": "Transaction créée avec succès",
  "createdAt": "2025-12-29T21:24:30"
}
```

**Response 422 (Unprocessable Entity) - Montant > 10 000€:**
```json
{
  "id": 2,
  "sourceAccountId": "ACC123456",
  "destinationIban": "FR1420041010050500013M02606",
  "amount": 15000.0,
  "status": "REJECTED",
  "message": "Transaction rejetée: montant supérieur au seuil autorisé (15000.00€ > 10000.00€)",
  "createdAt": "2025-12-29T21:24:30"
}
```

## 📝 Exemple avec cURL

### Virement standard (montant normal)

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountId": "ACC123456",
    "destinationIban": "FR1420041010050500013M02606",
    "amount": 500.0,
    "type": "STANDARD"
  }'
```

### Virement rejeté (montant > 10 000€)

```bash
curl -X POST http://localhost:8080/api/v1/payments \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccountId": "ACC123456",
    "destinationIban": "FR1420041010050500013M02606",
    "amount": 15000.0,
    "type": "STANDARD"
  }'
```

## 🔍 Validation

Les validations suivantes sont appliquées:

- `sourceAccountId`: Requis, max 50 caractères
- `destinationIban`: Requis, max 34 caractères
- `amount`: Requis, doit être > 0.01
- `type`: Requis (STANDARD ou INSTANT)

## 📊 Statuts de Transaction

- **PENDING**: Transaction créée, en attente de traitement automatique
- **VALIDATED**: Transaction validée par le legacy-adapter-service
- **REJECTED**: Transaction rejetée définitivement (règles anti-fraude 1 ou 2)
- **PENDING_MANUAL_REVIEW**: Transaction en attente de validation manuelle (règle anti-fraude 3 - nouveau bénéficiaire)
- **COMPLETED**: Transaction complétée (non utilisé dans cette version)

## 🎯 Prochaines Étapes

- [ ] Intégration réelle avec `legacy-adapter-service` (SOAP)
- [ ] Intégration avec `account-service` pour vérification de solde
- [ ] Implémentation du pattern Saga pour la compensation
- [ ] Ajout de plus de règles anti-fraude (vélocité, blacklist, etc.)
- [ ] Endpoint GET pour consulter les transactions

