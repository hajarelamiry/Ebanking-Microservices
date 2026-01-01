# Script de test complet pour valider le mapping userId -> accountRef
# Vérifie d'abord que les services sont démarrés, puis exécute les tests

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Magenta
Write-Host "Test Complet - Mapping userId -> accountRef" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

# Configuration
$keycloakUrl = "http://localhost:8180"
$accountServiceUrl = "http://localhost:8087"
$cryptoServiceUrl = "http://localhost:8085"
$eurekaUrl = "http://localhost:8761"

# Fonction pour vérifier si un service est démarré
function Test-ServiceRunning {
    param(
        [string]$ServiceName,
        [string]$Url,
        [int]$TimeoutSec = 2
    )
    
    try {
        $response = Invoke-WebRequest -Uri $Url -Method GET -TimeoutSec $TimeoutSec -ErrorAction Stop
        Write-Host "✅ $ServiceName est accessible" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "❌ $ServiceName n'est pas accessible sur $Url" -ForegroundColor Red
        return $false
    }
}

# Vérification des services
Write-Host "📝 Vérification des services..." -ForegroundColor Cyan
Write-Host ""

$servicesOk = $true

# Vérifier Keycloak
if (-not (Test-ServiceRunning -ServiceName "Keycloak" -Url "$keycloakUrl/realms/ebanking-realm")) {
    $servicesOk = $false
}

# Vérifier Eureka
if (-not (Test-ServiceRunning -ServiceName "Eureka Server" -Url $eurekaUrl)) {
    Write-Host "⚠️  Eureka Server n'est pas accessible (peut être normal si non démarré)" -ForegroundColor Yellow
}

# Vérifier account-service
if (-not (Test-ServiceRunning -ServiceName "Account Service" -Url "$accountServiceUrl/actuator/health")) {
    if (-not (Test-ServiceRunning -ServiceName "Account Service" -Url "$accountServiceUrl/api/accounts")) {
        $servicesOk = $false
    }
}

# Vérifier crypto-service
if (-not (Test-ServiceRunning -ServiceName "Crypto Service" -Url "$cryptoServiceUrl/actuator/health")) {
    if (-not (Test-ServiceRunning -ServiceName "Crypto Service" -Url "$cryptoServiceUrl/api/v1/crypto/prices")) {
        $servicesOk = $false
    }
}

Write-Host ""

if (-not $servicesOk) {
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "❌ Certains services ne sont pas démarrés!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Démarrez les services suivants:" -ForegroundColor Yellow
    Write-Host "   1. Keycloak sur le port 8180" -ForegroundColor Yellow
    Write-Host "   2. Eureka Server sur le port 8761" -ForegroundColor Yellow
    Write-Host "   3. account-service sur le port 8087" -ForegroundColor Yellow
    Write-Host "   4. crypto-service sur le port 8085" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Ensuite, relancez ce script." -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Tous les services sont accessibles!" -ForegroundColor Green
Write-Host ""

# Demander confirmation avant de lancer les tests
Write-Host "📝 Prêt à lancer les tests?" -ForegroundColor Cyan
Write-Host "   Les scripts suivants seront exécutés:" -ForegroundColor Cyan
Write-Host "   1. test-account-by-userid.ps1" -ForegroundColor Cyan
Write-Host "   2. test-crypto-transactions.ps1" -ForegroundColor Cyan
Write-Host ""
$confirmation = Read-Host "Appuyez sur Entree pour continuer ou 'N' pour annuler"

if ($confirmation -eq "N" -or $confirmation -eq "n") {
    Write-Host "Tests annulés." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test 1: Endpoint GET /api/accounts/user/{userId}" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Exécuter le premier test
try {
    & ".\test-account-by-userid.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "❌ Le premier test a échoué. Vérifiez les erreurs ci-dessus." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Erreur lors de l'exécution du premier test: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test 2: Transactions Crypto avec Mapping" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Exécuter le deuxième test
try {
    & ".\test-crypto-transactions.ps1"
    if ($LASTEXITCODE -ne 0) {
        Write-Host ""
        Write-Host "❌ Le deuxième test a échoué. Vérifiez les erreurs ci-dessus." -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Erreur lors de l'exécution du deuxième test: $_" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "✅ Tous les tests sont terminés!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📌 Résumé:" -ForegroundColor Cyan
Write-Host "   ✅ Le mapping userId -> accountRef fonctionne correctement" -ForegroundColor Green
Write-Host "   ✅ L'endpoint GET /api/accounts/user/{userId} est operationnel" -ForegroundColor Green
Write-Host "   ✅ Les transactions crypto utilisent le mapping correctement" -ForegroundColor Green
Write-Host ""
