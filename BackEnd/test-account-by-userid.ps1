# Script de test pour valider l'endpoint GET /api/accounts/user/{userId}
# Ce script teste le nouveau mapping userId -> accountRef

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test: GET /api/accounts/user/{userId}" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$keycloakUrl = "http://localhost:8080"
$realm = "ebanking-realm"
$clientId = "ebanking-client"
$clientSecret = "your-client-secret"
$accountServiceUrl = "http://localhost:8087"

# Fonction pour obtenir un token Keycloak
function Get-KeycloakToken {
    param(
        [string]$Username,
        [string]$Password
    )
    
    $tokenUrl = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"
    
    $body = @{
        grant_type = "password"
        client_id = $clientId
        client_secret = $clientSecret
        username = $Username
        password = $Password
    }
    
    try {
        $response = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
        return $response.access_token
    } catch {
        Write-Host "❌ Erreur lors de l'obtention du token: $_" -ForegroundColor Red
        return $null
    }
}

# Fonction pour tester l'endpoint
function Test-GetAccountByUserId {
    param(
        [string]$UserId,
        [string]$Token
    )
    
    $url = "$accountServiceUrl/api/accounts/user/$UserId"
    $headers = @{
        "Authorization" = "Bearer $Token"
        "Content-Type" = "application/json"
    }
    
    try {
        Write-Host "🔍 Test: GET $url" -ForegroundColor Yellow
        $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers
        
        Write-Host "✅ Succes!" -ForegroundColor Green
        Write-Host "   Account Reference: $($response.externalReference)" -ForegroundColor Green
        Write-Host "   Balance: $($response.balance) $($response.devise)" -ForegroundColor Green
        Write-Host "   Status: $($response.status)" -ForegroundColor Green
        return $true
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "❌ Erreur: Status $statusCode" -ForegroundColor Red
        Write-Host "   Message: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Test 1: Obtenir un token pour un utilisateur
Write-Host "📝 Etape 1: Obtention du token Keycloak" -ForegroundColor Cyan
Write-Host ""

# Remplacez par les identifiants réels de votre utilisateur de test
$testUsername = "testuser"
$testPassword = "testpassword"

$token = Get-KeycloakToken -Username $testUsername -Password $testPassword

if ($null -eq $token) {
    Write-Host "❌ Impossible d'obtenir le token. Vérifiez les identifiants Keycloak." -ForegroundColor Red
    exit 1
}

Write-Host "✅ Token obtenu avec succès" -ForegroundColor Green
Write-Host ""

# Test 2: Tester l'endpoint avec le userId
Write-Host "📝 Etape 2: Test de l'endpoint GET /api/accounts/user/{userId}" -ForegroundColor Cyan
Write-Host ""

# Utiliser le même username que userId (selon votre configuration Keycloak)
$userId = $testUsername

$success = Test-GetAccountByUserId -UserId $userId -Token $token

Write-Host ""
if ($success) {
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "✅ Test reussi!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "📌 Le mapping userId -> accountRef fonctionne correctement." -ForegroundColor Cyan
    Write-Host "   Le crypto-service peut maintenant utiliser cet endpoint" -ForegroundColor Cyan
    Write-Host "   au lieu de userId.toString() pour obtenir l'accountRef." -ForegroundColor Cyan
} else {
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "❌ Test echoue!" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Verifiez:" -ForegroundColor Yellow
    Write-Host "   1. Que account-service est demarre sur le port 8087" -ForegroundColor Yellow
    Write-Host "   2. Que l'utilisateur a un compte cree dans account-service" -ForegroundColor Yellow
    Write-Host "   3. Que les identifiants Keycloak sont corrects" -ForegroundColor Yellow
    Write-Host "   4. Que Keycloak est demarre sur le port 8180" -ForegroundColor Yellow
    exit 1
}
