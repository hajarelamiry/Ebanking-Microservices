# Script de test complet pour tous les services avec tokens JWT
# Basé sur la configuration simple comme account-service

$baseUrl = "http://localhost:8180"
$realm = "ebanking-realm"
$clientId = "ebanking-client"
$clientSecret = "your-client-secret"

# Fonction pour obtenir un token Keycloak
function Get-KeycloakToken {
    param(
        [string]$username,
        [string]$password
    )
    
    $tokenUrl = "$baseUrl/realms/$realm/protocol/openid-connect/token"
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
        Write-Host "Erreur lors de l'obtention du token pour $username : $_" -ForegroundColor Red
        return $null
    }
}

# Fonction pour tester un endpoint
function Test-Endpoint {
    param(
        [string]$serviceName,
        [string]$url,
        [string]$method = "GET",
        [string]$token = $null,
        [object]$body = $null,
        [int]$expectedStatus = 200
    )
    
    $headers = @{}
    if ($token) {
        $headers["Authorization"] = "Bearer $token"
    }
    
    try {
        if ($method -eq "GET") {
            $response = Invoke-WebRequest -Uri $url -Method $method -Headers $headers -ErrorAction SilentlyContinue
        } else {
            $jsonBody = if ($body) { $body | ConvertTo-Json } else { $null }
            $response = Invoke-WebRequest -Uri $url -Method $method -Headers $headers -Body $jsonBody -ContentType "application/json" -ErrorAction SilentlyContinue
        }
        
        $status = $response.StatusCode
        $success = ($status -eq $expectedStatus)
        $statusText = if ($success) { "SUCCESS" } else { "FAILED" }
        $color = if ($success) { "Green" } else { "Red" }
        
        Write-Host "[$serviceName] $method $url" -ForegroundColor Cyan
        Write-Host "  Status: $status ($statusText) | Attendu: $expectedStatus" -ForegroundColor $color
        Write-Host "  Token: $(if ($token) { 'Present' } else { 'Absent' })" -ForegroundColor Gray
        
        return $success
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        $success = ($status -eq $expectedStatus)
        $statusText = if ($success) { "SUCCESS" } else { "FAILED" }
        $color = if ($success) { "Green" } else { "Red" }
        
        Write-Host "[$serviceName] $method $url" -ForegroundColor Cyan
        Write-Host "  Status: $status ($statusText) | Attendu: $expectedStatus" -ForegroundColor $color
        Write-Host "  Token: $(if ($token) { 'Present' } else { 'Absent' })" -ForegroundColor Gray
        Write-Host "  Erreur: $($_.Exception.Message)" -ForegroundColor Yellow
        
        return $success
    }
}

Write-Host "========================================" -ForegroundColor Magenta
Write-Host "Test Complet - Tous les Services JWT" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

# Obtenir des tokens pour différents rôles
Write-Host "=== Obtention des tokens ===" -ForegroundColor Yellow
$tokenClient = Get-KeycloakToken -username "client1" -password "password"
$tokenAgent = Get-KeycloakToken -username "agent1" -password "password"
$tokenAdmin = Get-KeycloakToken -username "admin1" -password "password"

if (-not $tokenClient -and -not $tokenAgent -and -not $tokenAdmin) {
    Write-Host "ATTENTION: Impossible d'obtenir les tokens. Utilisation d'un token invalide pour les tests." -ForegroundColor Yellow
    $tokenInvalid = "invalid-token-for-testing"
}

Write-Host ""
Write-Host "=== ACCOUNT SERVICE (Port 8087) ===" -ForegroundColor Green

# Tests sans token (doivent retourner 401)
Test-Endpoint -serviceName "ACCOUNT" -url "http://localhost:8087/api/accounts" -method "GET" -expectedStatus 401
Test-Endpoint -serviceName "ACCOUNT" -url "http://localhost:8087/api/accounts/test/balance" -method "GET" -expectedStatus 401

# Tests avec token valide
if ($tokenClient) {
    Test-Endpoint -serviceName "ACCOUNT" -url "http://localhost:8087/api/accounts" -method "GET" -token $tokenClient -expectedStatus 200
}

Write-Host ""
Write-Host "=== PAYMENT SERVICE (Port 8086) ===" -ForegroundColor Green

# Tests sans token (doivent retourner 401)
Test-Endpoint -serviceName "PAYMENT" -url "http://localhost:8086/api/v1/payments" -method "GET" -expectedStatus 401
Test-Endpoint -serviceName "PAYMENT" -url "http://localhost:8086/api/v1/payments/initiate" -method "POST" -body @{amount=100; currency="EUR"} -expectedStatus 401

# Tests avec token invalide (doivent retourner 401)
Test-Endpoint -serviceName "PAYMENT" -url "http://localhost:8086/api/v1/payments" -method "GET" -token "invalid-token" -expectedStatus 401

# Tests avec token valide
if ($tokenClient) {
    Test-Endpoint -serviceName "PAYMENT" -url "http://localhost:8086/api/v1/payments" -method "GET" -token $tokenClient -expectedStatus 200
}

Write-Host ""
Write-Host "=== CRYPTO SERVICE (Port 8085) ===" -ForegroundColor Green

# Tests sans token (doivent retourner 401)
Test-Endpoint -serviceName "CRYPTO" -url "http://localhost:8085/api/v1/crypto/prices" -method "GET" -expectedStatus 401
Test-Endpoint -serviceName "CRYPTO" -url "http://localhost:8085/api/v1/crypto/wallet" -method "GET" -expectedStatus 401

# Tests avec token invalide (doivent retourner 401)
Test-Endpoint -serviceName "CRYPTO" -url "http://localhost:8085/api/v1/crypto/prices" -method "GET" -token "invalid-token" -expectedStatus 401

# Tests avec token valide
if ($tokenClient) {
    Test-Endpoint -serviceName "CRYPTO" -url "http://localhost:8085/api/v1/crypto/prices" -method "GET" -token $tokenClient -expectedStatus 200
}

Write-Host ""
Write-Host "=== AUDIT SERVICE (Port 8084) ===" -ForegroundColor Green

# Tests des endpoints internes (doivent fonctionner sans token)
Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/log" -method "POST" -body @{serviceName="test"; action="test"; userId="1"} -expectedStatus 200
Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/events" -method "POST" -body @{serviceName="test"; action="test"; userId="1"} -expectedStatus 201

# Tests des endpoints utilisateurs sans token (doivent retourner 401)
Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/users/1/history" -method "GET" -expectedStatus 401
Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/history" -method "GET" -expectedStatus 401
Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/errors" -method "GET" -expectedStatus 401
Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/health" -method "GET" -expectedStatus 401

# Tests avec token invalide (doivent retourner 401)
Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/users/1/history" -method "GET" -token "invalid-token" -expectedStatus 401

# Tests avec token valide
if ($tokenClient) {
    Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/users/1/history" -method "GET" -token $tokenClient -expectedStatus 200
}
if ($tokenAdmin) {
    Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/history" -method "GET" -token $tokenAdmin -expectedStatus 200
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "Tests terminés!" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
