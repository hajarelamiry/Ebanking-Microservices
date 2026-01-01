# 🐳 Guide Docker - E-Banking Microservices

Ce guide explique comment démarrer tous les microservices de l'application E-Banking en utilisant Docker et Docker Compose.

## 📋 Prérequis

- **Docker** version 20.10 ou supérieure
- **Docker Compose** version 2.0 ou supérieure
- Au moins **8 GB de RAM** disponible
- Au moins **10 GB d'espace disque** disponible

## 🚀 Démarrage Rapide

### 1. Démarrer tous les services

```bash
docker-compose up -d
```

Cette commande va :
- Construire automatiquement `security-common` pour les services qui en ont besoin
- Construire les images Docker pour tous les microservices
- Démarrer tous les conteneurs (Keycloak, Eureka, bases de données, Kafka, microservices)
- Configurer le réseau Docker pour la communication inter-services

**Note** : Le module `security-common` est construit automatiquement dans chaque Dockerfile qui en a besoin. Vous n'avez pas besoin de l'installer manuellement.

### 2. Vérifier le statut des services

```bash
docker-compose ps
```

### 3. Voir les logs

```bash
# Tous les services
docker-compose logs -f

# Un service spécifique
docker-compose logs -f user-service
docker-compose logs -f api-gateway
```

### 4. Arrêter tous les services

```bash
docker-compose down
```

### 5. Arrêter et supprimer les volumes (⚠️ supprime les données)

```bash
docker-compose down -v
```

## 📊 Architecture Docker

### Services Infrastructure

| Service | Port | Description |
|---------|------|-------------|
| **Keycloak** | 8080 | Serveur d'authentification OAuth2 |
| **Eureka Server** | 8761 | Service Discovery |
| **API Gateway** | 8088 | Point d'entrée unique pour toutes les APIs |

### Microservices

| Service | Port | Base de données | Description |
|---------|------|-----------------|-------------|
| **Auth Service** | 8081 | - | Service d'authentification |
| **User Service** | 8082 | user-db (5433) | Gestion des utilisateurs |
| **Account Service** | 8087 | account-db (5434) | Gestion des comptes bancaires |
| **Payment Service** | 8086 | payment-db (5435) | Gestion des virements |
| **Crypto Service** | 8085 | crypto-db (5436) | Trading de cryptomonnaies |
| **Audit Service** | 8084 | audit-db (5437) | Journalisation des événements |

### Bases de données PostgreSQL

Chaque microservice a sa propre base de données PostgreSQL :

- `user-db` : Port 5433
- `account-db` : Port 5434
- `payment-db` : Port 5435
- `crypto-db` : Port 5436
- `audit-db` : Port 5437
- `keycloak-db` : Base de données interne pour Keycloak

### Kafka

- **Zookeeper** : Port 2181 (interne)
- **Kafka** : Port 9092 (exposé pour les tests)

## 🔧 Configuration

### Variables d'environnement

Les variables d'environnement sont configurées dans le fichier `docker-compose.yml`. Les principales configurations :

- **Keycloak** : `http://keycloak:8080/realms/ebanking-realm`
- **Eureka** : `http://eureka-server:8761/eureka/`
- **Kafka** : `kafka:29092` (interne) ou `localhost:9092` (externe)

### Réseau Docker

Tous les services communiquent via le réseau `ebanking-network` qui permet :
- La découverte automatique des services par nom
- La communication sécurisée entre conteneurs
- L'isolation du reste du système

## 📝 Commandes Utiles

### Reconstruire un service spécifique

```bash
docker-compose build user-service
docker-compose up -d user-service
```

### Redémarrer un service

```bash
docker-compose restart user-service
```

### Accéder aux logs d'un service

```bash
docker-compose logs -f --tail=100 user-service
```

### Accéder à un shell dans un conteneur

```bash
docker-compose exec user-service sh
```

### Vérifier la santé des services

```bash
# Eureka Dashboard
open http://localhost:8761

# Keycloak Admin Console
open http://localhost:8080
# Login: admin / admin

# API Gateway
open http://localhost:8088
```

## 🧪 Tests

### Tester l'API Gateway

```bash
# Obtenir un token JWT
curl -X POST http://localhost:8080/realms/ebanking-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=ebanking-client&username=user1&password=password"

# Utiliser le token pour appeler l'API Gateway
curl -X GET http://localhost:8088/api/customers/me \
  -H "Authorization: Bearer <TOKEN>"
```

### Vérifier les services dans Eureka

Ouvrir http://localhost:8761 dans votre navigateur. Vous devriez voir tous les microservices enregistrés.

## 🐛 Dépannage

### Les services ne démarrent pas

1. **Vérifier les logs** :
   ```bash
   docker-compose logs <service-name>
   ```

2. **Vérifier l'espace disque** :
   ```bash
   docker system df
   ```

3. **Nettoyer Docker** :
   ```bash
   docker system prune -a
   ```

### Erreur de connexion à la base de données

1. Vérifier que la base de données est démarrée :
   ```bash
   docker-compose ps | grep db
   ```

2. Vérifier les logs de la base de données :
   ```bash
   docker-compose logs user-db
   ```

### Erreur de connexion à Keycloak

1. Attendre que Keycloak soit complètement démarré (peut prendre 1-2 minutes)
2. Vérifier les logs :
   ```bash
   docker-compose logs keycloak
   ```

### Erreur de connexion à Eureka

1. Vérifier que Eureka est démarré :
   ```bash
   docker-compose logs eureka-server
   ```

2. Accéder au dashboard : http://localhost:8761

### Les services ne se découvrent pas via Eureka

1. Vérifier que tous les services ont la bonne URL Eureka :
   ```bash
   docker-compose exec user-service env | grep EUREKA
   ```

2. Vérifier le dashboard Eureka : http://localhost:8761

## 📦 Build des Images

### Build manuel d'un service

```bash
cd account-service
docker build -t ebanking-account-service .
```

### Build de tous les services

```bash
docker-compose build
```

### Build sans cache

```bash
docker-compose build --no-cache
```

## 🔒 Sécurité

### Production

⚠️ **Ce docker-compose.yml est configuré pour le développement uniquement.**

Pour la production, vous devez :

1. **Changer tous les mots de passe** par défaut
2. **Utiliser des secrets Docker** pour les credentials
3. **Configurer HTTPS** pour tous les services
4. **Restreindre les ports exposés**
5. **Utiliser un reverse proxy** (Nginx, Traefik)
6. **Configurer des limites de ressources** (CPU, RAM)

## 📚 Ressources

- [Documentation Docker](https://docs.docker.com/)
- [Documentation Docker Compose](https://docs.docker.com/compose/)
- [Spring Cloud Eureka](https://spring.io/projects/spring-cloud-netflix)
- [Keycloak Documentation](https://www.keycloak.org/documentation)

## 🆘 Support

En cas de problème :

1. Vérifier les logs : `docker-compose logs -f`
2. Vérifier le statut : `docker-compose ps`
3. Vérifier les ressources : `docker stats`
4. Consulter la documentation ci-dessus

---

**Note** : Le premier démarrage peut prendre 5-10 minutes car Docker doit :
- Télécharger toutes les images de base
- Construire les images des microservices
- Initialiser toutes les bases de données
- Démarrer tous les services

Soyez patient ! 🚀
