# Script de test pour l'authentification JWT/Keycloak
# Teste les APIs avec et sans token, et avec différents rôles

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test d'Authentification JWT/Keycloak" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$keycloakUrl = "http://localhost:8080"
$realm = "ebanking-realm"
$paymentServiceUrl = "http://localhost:8086"
$cryptoServiceUrl = "http://localhost:8085"
$auditServiceUrl = "http://localhost:8084"

# Fonction pour obtenir un token JWT depuis Keycloak
function Get-KeycloakToken {
    param(
        [string]$clientId,
        [string]$clientSecret,
        [string]$username,
        [string]$password
    )
    
    $tokenUrl = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"
    
    $body = @{
        grant_type = "password"
        client_id = $clientId
        client_secret = $clientSecret
        username = $username
        password = $password
    }
    
    try {
        $response = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
        return $response.access_token
    } catch {
        Write-Host "Erreur lors de l'obtention du token: $_" -ForegroundColor Red
        return $null
    }
}

# Fonction pour tester un endpoint
function Test-Endpoint {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Token = $null,
        [string]$Body = $null,
        [string]$Description
    )
    
    Write-Host "`nTest: $Description" -ForegroundColor Yellow
    Write-Host "URL: $Url" -ForegroundColor Gray
    Write-Host "Method: $Method" -ForegroundColor Gray
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
        Write-Host "Token: Present" -ForegroundColor Green
    } else {
        Write-Host "Token: Absent" -ForegroundColor Red
    }
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $headers
        }
        
        if ($Body) {
            $params["Body"] = $Body
        }
        
        $response = Invoke-RestMethod @params -ErrorAction Stop
        Write-Host "Status: SUCCESS" -ForegroundColor Green
        Write-Host "Response: $($response | ConvertTo-Json -Depth 2)" -ForegroundColor Gray
        return $true
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $errorMessage = $_.Exception.Message
        Write-Host "Status: FAILED (HTTP $statusCode)" -ForegroundColor Red
        Write-Host "Error: $errorMessage" -ForegroundColor Red
        
        # Afficher le body de l'erreur si disponible
        if ($_.Exception.Response) {
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $responseBody = $reader.ReadToEnd()
            if ($responseBody) {
                Write-Host "Response Body: $responseBody" -ForegroundColor Red
            }
        }
        return $false
    }
}

Write-Host "`n=== ÉTAPE 1: Test sans token (doit échouer) ===" -ForegroundColor Cyan

# Test Payment Service sans token
Test-Endpoint -Method "POST" -Url "$paymentServiceUrl/api/v1/payments" `
    -Body '{"sourceAccountId":"123","destinationAccountId":"456","amount":100.0,"type":"TRANSFER"}' `
    -Description "Payment Service - POST /api/v1/payments (SANS TOKEN)"

# Test Crypto Service sans token
Test-Endpoint -Method "GET" -Url "$cryptoServiceUrl/api/v1/crypto/prices" `
    -Description "Crypto Service - GET /api/v1/crypto/prices (SANS TOKEN)"

# Test Audit Service sans token (endpoint interne doit fonctionner)
Test-Endpoint -Method "POST" -Url "$auditServiceUrl/api/audit/events" `
    -Body '{"serviceName":"test","actionType":"TEST","userId":"1","status":"SUCCESS","message":"Test"}' `
    -Description "Audit Service - POST /api/audit/events (SANS TOKEN - doit fonctionner car interne)"

# Test Audit Service sans token (endpoint utilisateur doit échouer)
Test-Endpoint -Method "GET" -Url "$auditServiceUrl/api/audit/users/1/history" `
    -Description "Audit Service - GET /api/audit/users/1/history (SANS TOKEN - doit échouer)"

Write-Host "`n=== ÉTAPE 2: Obtention d'un token JWT depuis Keycloak ===" -ForegroundColor Cyan
Write-Host "Note: Vous devez configurer les credentials Keycloak ci-dessous" -ForegroundColor Yellow
Write-Host ""

# Configuration Keycloak (à adapter selon votre configuration)
$clientId = "ebanking-client"  # À adapter
$clientSecret = ""  # À adapter si client est confidentiel
$username = "test-user"  # À adapter
$password = "test-password"  # À adapter

Write-Host "Tentative d'obtention d'un token avec:" -ForegroundColor Yellow
Write-Host "  Client ID: $clientId" -ForegroundColor Gray
Write-Host "  Username: $username" -ForegroundColor Gray

$token = Get-KeycloakToken -clientId $clientId -clientSecret $clientSecret -username $username -password $password

if ($token) {
    Write-Host "Token obtenu avec succès!" -ForegroundColor Green
    Write-Host "Token (premiers 50 caractères): $($token.Substring(0, [Math]::Min(50, $token.Length)))..." -ForegroundColor Gray
    
    Write-Host "`n=== ÉTAPE 3: Test avec token (doit réussir) ===" -ForegroundColor Cyan
    
    # Test Payment Service avec token
    Test-Endpoint -Method "POST" -Url "$paymentServiceUrl/api/v1/payments" `
        -Token $token `
        -Body '{"sourceAccountId":"123","destinationAccountId":"456","amount":100.0,"type":"TRANSFER"}' `
        -Description "Payment Service - POST /api/v1/payments (AVEC TOKEN)"
    
    # Test Crypto Service avec token
    Test-Endpoint -Method "GET" -Url "$cryptoServiceUrl/api/v1/crypto/prices" `
        -Token $token `
        -Description "Crypto Service - GET /api/v1/crypto/prices (AVEC TOKEN)"
    
    # Test Crypto Service - Wallet avec token
    Test-Endpoint -Method "GET" -Url "$cryptoServiceUrl/api/v1/crypto/wallet?userId=1" `
        -Token $token `
        -Description "Crypto Service - GET /api/v1/crypto/wallet (AVEC TOKEN)"
    
    # Test Audit Service avec token
    Test-Endpoint -Method "GET" -Url "$auditServiceUrl/api/audit/users/1/history" `
        -Token $token `
        -Description "Audit Service - GET /api/audit/users/1/history (AVEC TOKEN)"
    
    # Test Audit Service - Health avec token
    Test-Endpoint -Method "GET" -Url "$auditServiceUrl/api/audit/health" `
        -Token $token `
        -Description "Audit Service - GET /api/audit/health (AVEC TOKEN)"
    
    Write-Host "`n=== ÉTAPE 4: Test des restrictions de rôles ===" -ForegroundColor Cyan
    Write-Host "Note: Ces tests nécessitent des tokens avec différents rôles" -ForegroundColor Yellow
    
    # Test Crypto Trade (doit être CLIENT uniquement)
    Test-Endpoint -Method "POST" -Url "$cryptoServiceUrl/api/v1/crypto/trade?userId=1" `
        -Token $token `
        -Body '{"symbol":"BTC","quantity":0.1,"type":"BUY"}' `
        -Description "Crypto Service - POST /api/v1/crypto/trade (doit être CLIENT)"
    
    # Test Audit History (doit être ADMIN uniquement)
    Test-Endpoint -Method "GET" -Url "$auditServiceUrl/api/audit/history" `
        -Token $token `
        -Description "Audit Service - GET /api/audit/history (doit être ADMIN)"
    
    # Test Audit Errors (doit être AGENT ou ADMIN)
    Test-Endpoint -Method "GET" -Url "$auditServiceUrl/api/audit/errors" `
        -Token $token `
        -Description "Audit Service - GET /api/audit/errors (doit être AGENT ou ADMIN)"
    
} else {
    Write-Host "`nImpossible d'obtenir un token. Vérifiez:" -ForegroundColor Red
    Write-Host "  1. Keycloak est démarré sur $keycloakUrl" -ForegroundColor Yellow
    Write-Host "  2. Le realm '$realm' existe" -ForegroundColor Yellow
    Write-Host "  3. Le client '$clientId' est configuré" -ForegroundColor Yellow
    Write-Host "  4. Les credentials utilisateur sont corrects" -ForegroundColor Yellow
    Write-Host "`nPour tester manuellement, utilisez:" -ForegroundColor Cyan
    Write-Host "  curl -X POST '$keycloakUrl/realms/$realm/protocol/openid-connect/token' \`" -ForegroundColor Gray
    Write-Host "    -d 'grant_type=password&client_id=$clientId&username=$username&password=$password'" -ForegroundColor Gray
}

Write-Host "`n=== ÉTAPE 5: Test des endpoints internes (sans token) ===" -ForegroundColor Cyan

# Test des endpoints internes qui doivent fonctionner sans token
Test-Endpoint -Method "POST" -Url "$auditServiceUrl/api/audit/log" `
    -Body '{"serviceName":"payment-service","actionType":"PAYMENT_CREATED","userId":"1","status":"SUCCESS","message":"Test"}' `
    -Description "Audit Service - POST /api/audit/log (INTERNE - sans token)"

Test-Endpoint -Method "POST" -Url "$auditServiceUrl/api/audit/events/external" `
    -Body '{"serviceName":"crypto-service","actionType":"CRYPTO_TRADE","userId":"1","status":"SUCCESS","message":"Test"}' `
    -Description "Audit Service - POST /api/audit/events/external (INTERNE - sans token)"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Tests terminés!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
