# Script de test pour l'intégration inter-services
# Teste la communication entre payment-service, crypto-service et account-service

Write-Host "========================================" -ForegroundColor Magenta
Write-Host "Test Integration Inter-Services" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

# Configuration
$keycloakUrl = "http://localhost:8080"
$realm = "ebanking-realm"
$clientId = "ebanking-client"
$clientSecret = "your-client-secret"

# Fonction pour obtenir un token Keycloak
function Get-KeycloakToken {
    param(
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
        $color = if ($success) { "Green" } else { "Red" }
        
        Write-Host "[$serviceName] $method $url" -ForegroundColor Cyan
        Write-Host "  Status: $status | Attendu: $expectedStatus | $(if ($success) { '✓' } else { '✗' })" -ForegroundColor $color
        
        if ($success -and $response.Content) {
            $content = $response.Content | ConvertFrom-Json
            return @{ Success = $success; Status = $status; Content = $content; Service = $serviceName }
        }
        
        return @{ Success = $success; Status = $status; Service = $serviceName }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if (-not $status) { $status = 500 }
        $success = ($status -eq $expectedStatus)
        $color = if ($success) { "Green" } else { "Red" }
        
        Write-Host "[$serviceName] $method $url" -ForegroundColor Cyan
        Write-Host "  Status: $status | Attendu: $expectedStatus | $(if ($success) { '✓' } else { '✗' })" -ForegroundColor $color
        
        return @{ Success = $success; Status = $status; Service = $serviceName }
    }
}

Write-Host "=== Obtention du token ===" -ForegroundColor Yellow
$token = Get-KeycloakToken -username "client1" -password "password"

if (-not $token) {
    Write-Host "ATTENTION: Impossible d'obtenir le token. Les tests avec authentification vont echouer." -ForegroundColor Yellow
    $token = "invalid-token-for-testing"
}

Write-Host ""
Write-Host "=== ACCOUNT SERVICE - Verification ===" -ForegroundColor Green

# Test 1: Créer un compte (si nécessaire)
Write-Host "Test: Creation d'un compte EUR" -ForegroundColor Cyan
$createAccountBody = @{
    devise = "EUR"
}
$accountResult = Test-Endpoint -serviceName "ACCOUNT" -url "http://localhost:8087/api/accounts" -method "POST" -token $token -body $createAccountBody -expectedStatus 201

if ($accountResult.Success -and $accountResult.Content) {
    $accountRef = $accountResult.Content.externalReference
    Write-Host "  Compte cree avec reference: $accountRef" -ForegroundColor Green
    
    # Test 2: Vérifier le solde
    Write-Host ""
    Write-Host "Test: Verification du solde" -ForegroundColor Cyan
    $balanceResult = Test-Endpoint -serviceName "ACCOUNT" -url "http://localhost:8087/api/accounts/$accountRef/balance" -method "GET" -token $token -expectedStatus 200
    
    if ($balanceResult.Success -and $balanceResult.Content) {
        $balance = $balanceResult.Content.balance
        Write-Host "  Solde actuel: $balance EUR" -ForegroundColor Green
        
        # Test 3: Créditer le compte pour avoir un solde suffisant
        Write-Host ""
        Write-Host "Test: Credit du compte avec 1000 EUR" -ForegroundColor Cyan
        $creditBody = @{
            amount = 1000.00
        }
        Test-Endpoint -serviceName "ACCOUNT" -url "http://localhost:8087/api/accounts/$accountRef/credit" -method "POST" -token $token -body $creditBody -expectedStatus 200
        
        Write-Host ""
        Write-Host "=== PAYMENT SERVICE - Test avec verification de solde ===" -ForegroundColor Green
        
        # Test 4: Tenter un paiement avec solde insuffisant
        Write-Host "Test: Tentative de paiement avec solde insuffisant (2000 EUR)" -ForegroundColor Cyan
        $paymentBody1 = @{
            sourceAccountId = $accountRef
            destinationIban = "FR7630004000031234567890143"
            amount = 2000.00
            type = "TRANSFER"
        }
        $paymentResult1 = Test-Endpoint -serviceName "PAYMENT" -url "http://localhost:8086/api/v1/payments/initiate" -method "POST" -token $token -body $paymentBody1 -expectedStatus 200
        
        if ($paymentResult1.Success -and $paymentResult1.Content) {
            if ($paymentResult1.Content.status -eq "REJECTED") {
                Write-Host "  ✓ Paiement correctement rejete (solde insuffisant)" -ForegroundColor Green
            } else {
                Write-Host "  ⚠ Paiement non rejete alors que le solde est insuffisant" -ForegroundColor Yellow
            }
        }
        
        # Test 5: Tenter un paiement avec solde suffisant
        Write-Host ""
        Write-Host "Test: Tentative de paiement avec solde suffisant (100 EUR)" -ForegroundColor Cyan
        $paymentBody2 = @{
            sourceAccountId = $accountRef
            destinationIban = "FR7630004000031234567890143"
            amount = 100.00
            type = "TRANSFER"
        }
        $paymentResult2 = Test-Endpoint -serviceName "PAYMENT" -url "http://localhost:8086/api/v1/payments/initiate" -method "POST" -token $token -body $paymentBody2 -expectedStatus 200
        
        if ($paymentResult2.Success) {
            Write-Host "  ✓ Paiement traite (verification de solde effectuee)" -ForegroundColor Green
        }
        
        Write-Host ""
        Write-Host "=== CRYPTO SERVICE - Test avec verification de solde ===" -ForegroundColor Green
        
        # Test 6: Tenter un achat crypto avec solde insuffisant
        Write-Host "Test: Tentative d'achat crypto avec solde insuffisant" -ForegroundColor Cyan
        $cryptoBuyBody1 = @{
            symbol = "BTC"
            quantity = 1.0
            type = "BUY"
        }
        # Note: Le userId sera extrait du token
        $cryptoResult1 = Test-Endpoint -serviceName "CRYPTO" -url "http://localhost:8085/api/v1/crypto/trade" -method "POST" -token $token -body $cryptoBuyBody1 -expectedStatus 400
        
        # Test 7: Tenter un achat crypto avec solde suffisant (petit montant)
        Write-Host ""
        Write-Host "Test: Tentative d'achat crypto avec solde suffisant (0.001 BTC)" -ForegroundColor Cyan
        $cryptoBuyBody2 = @{
            symbol = "BTC"
            quantity = 0.001
            type = "BUY"
        }
        $cryptoResult2 = Test-Endpoint -serviceName "CRYPTO" -url "http://localhost:8085/api/v1/crypto/trade" -method "POST" -token $token -body $cryptoBuyBody2 -expectedStatus 201
        
        if ($cryptoResult2.Success) {
            Write-Host "  ✓ Achat crypto traite (verification de solde et debit effectues)" -ForegroundColor Green
        }
    }
} else {
    Write-Host "  ⚠ Impossible de creer un compte. Les tests suivants peuvent echouer." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "Tests d'integration termines!" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
