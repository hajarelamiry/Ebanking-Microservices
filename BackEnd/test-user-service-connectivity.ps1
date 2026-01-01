# Script de test pour vérifier la connectivité avec user-service

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Magenta
Write-Host "Test User-Service Connectivity" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

# Configuration
$keycloakUrl = "http://localhost:8080"
$realm = "ebanking-realm"
$clientId = "ebanking-client"
$userServiceUrl = "http://localhost:8082"
$cryptoServiceUrl = "http://localhost:8085"

# Identifiants
$testUsername = "user1"
$testPassword = "password"

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Keycloak: $keycloakUrl" -ForegroundColor Gray
Write-Host "  User Service: $userServiceUrl" -ForegroundColor Gray
Write-Host "  Crypto Service: $cryptoServiceUrl" -ForegroundColor Gray
Write-Host "  Username: $testUsername" -ForegroundColor Gray
Write-Host ""

# ETAPE 1: Authentification
Write-Host "ETAPE 1: Authentification via Keycloak" -ForegroundColor Cyan
Write-Host ""

$tokenUrl = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"
$body = @{
    grant_type = "password"
    client_id = $clientId
    username = $testUsername
    password = $testPassword
}

try {
    Write-Host "Tentative d'authentification..." -ForegroundColor Yellow
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
    $token = $tokenResponse.access_token
    Write-Host "OK - Authentification reussie!" -ForegroundColor Green
    Write-Host "Token (first 50 chars): $($token.Substring(0, [Math]::Min(50, $token.Length)))..." -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "ERREUR - Authentification echouee" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Reponse: $responseBody" -ForegroundColor Red
    }
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# ETAPE 2: Test direct user-service
Write-Host "ETAPE 2: Test direct de user-service" -ForegroundColor Cyan
Write-Host ""

$userInfoUrl = "$userServiceUrl/api/customers/me"
try {
    Write-Host "Test de l'endpoint: $userInfoUrl" -ForegroundColor Yellow
    $userInfo = Invoke-RestMethod -Uri $userInfoUrl -Method Get -Headers $headers
    Write-Host "OK - User-service accessible!" -ForegroundColor Green
    Write-Host "  ID: $($userInfo.id)" -ForegroundColor Green
    Write-Host "  Username: $($userInfo.username)" -ForegroundColor Green
    Write-Host "  Email: $($userInfo.email)" -ForegroundColor Green
    Write-Host "  KYC Status: $($userInfo.kycStatus)" -ForegroundColor Green
    Write-Host ""
    
    if ($userInfo.id -eq $null) {
        Write-Host "ATTENTION - L'ID utilisateur est null!" -ForegroundColor Yellow
    }
} catch {
    Write-Host "ERREUR - User-service non accessible" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errorBody = $reader.ReadToEnd()
        Write-Host "Reponse erreur: $errorBody" -ForegroundColor Red
        
        if ($statusCode -eq 401) {
            Write-Host "" -ForegroundColor Yellow
            Write-Host "💡 Le token JWT n'est pas accepte par user-service" -ForegroundColor Yellow
            Write-Host "   Verifiez la configuration Keycloak dans user-service" -ForegroundColor Yellow
        } elseif ($statusCode -eq 404) {
            Write-Host "" -ForegroundColor Yellow
            Write-Host "💡 L'endpoint /api/customers/me n'existe pas" -ForegroundColor Yellow
        }
    } else {
        Write-Host "Message: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "💡 User-service n'est peut-etre pas demarre ou non accessible" -ForegroundColor Yellow
    }
    Write-Host ""
}

# ETAPE 3: Test via crypto-service (simulation de l'appel Feign)
Write-Host "ETAPE 3: Test de l'endpoint trade de crypto-service" -ForegroundColor Cyan
Write-Host ""

$tradeUrl = "$cryptoServiceUrl/api/v1/crypto/trade"
$tradeBody = @{
    symbol = "BTC"
    quantity = 0.001
    type = "BUY"
} | ConvertTo-Json

try {
    Write-Host "Test de l'endpoint trade (qui devrait appeler user-service en interne)..." -ForegroundColor Yellow
    Write-Host "URL: $tradeUrl" -ForegroundColor Gray
    Write-Host "Body: $tradeBody" -ForegroundColor Gray
    Write-Host ""
    
    $tradeResponse = Invoke-RestMethod -Uri $tradeUrl -Method Post -Headers $headers -Body $tradeBody -ErrorAction Stop
    Write-Host "OK - Transaction effectuee!" -ForegroundColor Green
    Write-Host "Transaction ID: $($tradeResponse.id)" -ForegroundColor Green
} catch {
    Write-Host "ERREUR - Echec de la transaction" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "Status Code: $statusCode" -ForegroundColor Red
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errorBody = $reader.ReadToEnd()
        Write-Host "Reponse erreur: $errorBody" -ForegroundColor Red
        
        if ($statusCode -eq 400 -and $errorBody -like '*"id":-1*') {
            Write-Host "" -ForegroundColor Yellow
            Write-Host "💡 Cette erreur indique que crypto-service n'a pas pu recuperer l'ID utilisateur" -ForegroundColor Yellow
            Write-Host "   Verifiez les logs de crypto-service pour voir l'erreur exacte de l'appel Feign" -ForegroundColor Yellow
            Write-Host "   L'erreur peut etre:" -ForegroundColor Yellow
            Write-Host "   1. user-service retourne 401 (token non propage)" -ForegroundColor Gray
            Write-Host "   2. user-service n'est pas accessible via Eureka" -ForegroundColor Gray
            Write-Host "   3. L'endpoint /api/customers/me n'existe pas" -ForegroundColor Gray
        }
    } else {
        Write-Host "Message: $($_.Exception.Message)" -ForegroundColor Red
    }
    Write-Host ""
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Test termine!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "💡 Prochaines etapes:" -ForegroundColor Yellow
Write-Host "   1. Verifiez les logs de crypto-service pour voir l'erreur exacte" -ForegroundColor Gray
Write-Host "   2. Verifiez que user-service est bien enregistre dans Eureka" -ForegroundColor Gray
Write-Host "   3. Verifiez la configuration Keycloak dans user-service" -ForegroundColor Gray
Write-Host ""
