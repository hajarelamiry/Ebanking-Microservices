# Guide de Configuration Eureka Server

## 🎯 Architecture

```
Eureka Server (Port 8761)
    ↓
Payment Service ──Feign──> Audit Service
Crypto Service  ──Feign──> Audit Service
```

## 📋 Étapes de Configuration

### 1. Eureka Server

Le service Eureka Server est déjà configuré dans `eureka-server/`.

**Démarrer Eureka Server :**
```bash
cd eureka-server
mvn spring-boot:run
```

**Vérifier :** http://localhost:8761

### 2. Services Clients (Payment, Crypto, Audit)

Tous les services sont configurés comme clients Eureka avec :
- `@EnableEurekaClient` dans les classes principales
- Configuration dans `application.properties`
- Feign Clients pour la communication

### 3. Feign Clients

Les Feign Clients permettent de communiquer avec Audit Service sans connaître son adresse IP :

**Payment Service :**
- `AuditClient` : Interface Feign pour appeler audit-service
- `AuditService` : Service wrapper avec conversion de DTO

**Crypto Service :**
- `AuditClient` : Interface Feign pour appeler audit-service
- `AuditService` : Service wrapper avec conversion de DTO

### 4. Circuit Breaker (Resilience4j)

Configuration du Circuit Breaker pour éviter que les services ne bloquent si Audit Service est indisponible :

```properties
resilience4j.circuitbreaker.instances.auditService.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.auditService.wait-duration-in-open-state=10000
resilience4j.circuitbreaker.instances.auditService.sliding-window-size=10
```

## 🚀 Ordre de Démarrage

1. **Eureka Server** (Port 8761)
   ```bash
   cd eureka-server
   mvn spring-boot:run
   ```

2. **Audit Service** (Port 8083)
   ```bash
   cd audit-service
   mvn spring-boot:run
   ```
   Vérifier sur http://localhost:8761 qu'il apparaît

3. **Payment Service** (Port 8080)
   ```bash
   cd payment-service
   mvn spring-boot:run
   ```

4. **Crypto Service** (Port 8082)
   ```bash
   cd crypto-service
   mvn spring-boot:run
   ```

## 🔍 Vérification

### Vérifier que les services sont enregistrés dans Eureka

1. Ouvrir http://localhost:8761
2. Vous devriez voir :
   - `AUDIT-SERVICE`
   - `PAYMENT-SERVICE`
   - `CRYPTO-SERVICE`

### Tester la communication Feign

Les services peuvent maintenant utiliser `AuditClient` pour envoyer des événements :

```java
@Autowired
private AuditService auditService;

// Dans votre logique métier
auditService.sendAuditEvent(auditEventDTO);
```

## ⚙️ Configuration

### Payment Service & Crypto Service

```properties
# Eureka Client
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true

# Feign Client
feign.client.config.default.connect-timeout=5000
feign.client.config.default.read-timeout=5000
feign.circuitbreaker.enabled=true

# Resilience4j
resilience4j.circuitbreaker.instances.auditService.failure-rate-threshold=50
```

### Audit Service

```properties
# Eureka Client
eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
eureka.instance.prefer-ip-address=true
```

## 🔄 Double Communication (Kafka + Feign)

Les services utilisent maintenant **deux canaux** pour l'audit :

1. **Kafka (Asynchrone)** : Transactional Outbox Pattern
   - Fiabilité garantie
   - Pas de blocage
   - Utilisé par défaut

2. **Feign/Eureka (Synchrone)** : Communication directe
   - Découverte automatique via Eureka
   - Circuit Breaker pour la résilience
   - Optionnel (décommenter dans PaymentServiceImpl)

## 📝 Notes

- Les événements sont **toujours** envoyés via Kafka (fiabilité)
- Feign peut être utilisé en **complément** pour des cas spécifiques
- Le Circuit Breaker protège contre les pannes d'Audit Service
- Eureka découvre automatiquement les instances des services

