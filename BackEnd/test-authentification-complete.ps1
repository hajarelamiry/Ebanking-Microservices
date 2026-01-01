# Script de test complet pour l'authentification JWT/Keycloak
# Teste les APIs avec et sans token dans Payment, Crypto et Audit services

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test d'Authentification JWT/Keycloak" -ForegroundColor Cyan
Write-Host "Test complet des 3 services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$paymentServiceUrl = "http://localhost:8086"
$cryptoServiceUrl = "http://localhost:8085"
$auditServiceUrl = "http://localhost:8084"

$testResults = @()

# Fonction pour tester un endpoint
function Test-Endpoint {
    param(
        [string]$ServiceName,
        [string]$Method,
        [string]$Url,
        [string]$Token = $null,
        [string]$Body = $null,
        [string]$Description,
        [int]$ExpectedStatus = 200
    )
    
    Write-Host "`n[TEST] $ServiceName - $Description" -ForegroundColor Yellow
    Write-Host "  URL: $Url" -ForegroundColor Gray
    Write-Host "  Method: $Method" -ForegroundColor Gray
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
        Write-Host "  Token: Present" -ForegroundColor Green
    } else {
        Write-Host "  Token: Absent" -ForegroundColor Red
    }
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $headers
            ErrorAction = "Stop"
        }
        
        if ($Body) {
            $params["Body"] = $Body
        }
        
        $response = Invoke-RestMethod @params
        $statusCode = 200
        Write-Host "  Status: SUCCESS ($statusCode)" -ForegroundColor Green
        
        $testResults += @{
            Service = $ServiceName
            Test = $Description
            Status = "SUCCESS"
            Expected = $ExpectedStatus
            Actual = $statusCode
            Passed = ($statusCode -eq $ExpectedStatus)
        }
        return $true
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if (-not $statusCode) {
            $statusCode = 0
        }
        
        $color = if ($statusCode -eq $ExpectedStatus) { "Green" } else { "Red" }
        $symbol = if ($statusCode -eq $ExpectedStatus) { "✓" } else { "✗" }
        
        Write-Host "  Status: $symbol $statusCode (attendu: $ExpectedStatus)" -ForegroundColor $color
        
        $testResults += @{
            Service = $ServiceName
            Test = $Description
            Status = "FAILED"
            Expected = $ExpectedStatus
            Actual = $statusCode
            Passed = ($statusCode -eq $ExpectedStatus)
        }
        return ($statusCode -eq $ExpectedStatus)
    }
}

Write-Host "=== ÉTAPE 1: Tests sans token (doivent échouer avec 401) ===" -ForegroundColor Cyan

# Payment Service - Sans token
Test-Endpoint -ServiceName "Payment" -Method "POST" `
    -Url "$paymentServiceUrl/api/v1/payments" `
    -Body '{"sourceAccountId":"123","destinationAccountId":"456","amount":100.0,"type":"TRANSFER"}' `
    -Description "POST /api/v1/payments (SANS TOKEN)" `
    -ExpectedStatus 401

# Crypto Service - Sans token
Test-Endpoint -ServiceName "Crypto" -Method "GET" `
    -Url "$cryptoServiceUrl/api/v1/crypto/prices" `
    -Description "GET /api/v1/crypto/prices (SANS TOKEN)" `
    -ExpectedStatus 401

Test-Endpoint -ServiceName "Crypto" -Method "GET" `
    -Url "$cryptoServiceUrl/api/v1/crypto/wallet?userId=1" `
    -Description "GET /api/v1/crypto/wallet (SANS TOKEN)" `
    -ExpectedStatus 401

# Audit Service - Endpoint utilisateur sans token
Test-Endpoint -ServiceName "Audit" -Method "GET" `
    -Url "$auditServiceUrl/api/audit/users/1/history" `
    -Description "GET /api/audit/users/1/history (SANS TOKEN)" `
    -ExpectedStatus 401

Test-Endpoint -ServiceName "Audit" -Method "GET" `
    -Url "$auditServiceUrl/api/audit/history" `
    -Description "GET /api/audit/history (SANS TOKEN - ADMIN)" `
    -ExpectedStatus 401

Test-Endpoint -ServiceName "Audit" -Method "GET" `
    -Url "$auditServiceUrl/api/audit/errors" `
    -Description "GET /api/audit/errors (SANS TOKEN - AGENT/ADMIN)" `
    -ExpectedStatus 401

Write-Host "`n=== ÉTAPE 2: Tests des endpoints internes (doivent fonctionner sans token) ===" -ForegroundColor Cyan

# Audit Service - Endpoints internes
Test-Endpoint -ServiceName "Audit" -Method "POST" `
    -Url "$auditServiceUrl/api/audit/log" `
    -Body '{"serviceName":"payment-service","actionType":"PAYMENT_CREATED","userId":"1","status":"SUCCESS","message":"Test"}' `
    -Description "POST /api/audit/log (INTERNE - sans token)" `
    -ExpectedStatus 201

Test-Endpoint -ServiceName "Audit" -Method "POST" `
    -Url "$auditServiceUrl/api/audit/events" `
    -Body '{"serviceName":"test","actionType":"TEST","userId":"1","status":"SUCCESS","message":"Test"}' `
    -Description "POST /api/audit/events (INTERNE - sans token)" `
    -ExpectedStatus 201

Write-Host "`n=== ÉTAPE 3: Tests avec token invalide (doivent échouer avec 401) ===" -ForegroundColor Cyan

$invalidToken = "invalid-token-12345"

Test-Endpoint -ServiceName "Payment" -Method "POST" `
    -Url "$paymentServiceUrl/api/v1/payments" `
    -Token $invalidToken `
    -Body '{"sourceAccountId":"123","destinationAccountId":"456","amount":100.0,"type":"TRANSFER"}' `
    -Description "POST /api/v1/payments (TOKEN INVALIDE)" `
    -ExpectedStatus 401

Test-Endpoint -ServiceName "Crypto" -Method "GET" `
    -Url "$cryptoServiceUrl/api/v1/crypto/prices" `
    -Token $invalidToken `
    -Description "GET /api/v1/crypto/prices (TOKEN INVALIDE)" `
    -ExpectedStatus 401

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Résumé des tests" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$totalTests = $testResults.Count
$passedTests = ($testResults | Where-Object { $_.Passed -eq $true }).Count
$failedTests = $totalTests - $passedTests

Write-Host "Total: $totalTests tests" -ForegroundColor White
Write-Host "Réussis: $passedTests" -ForegroundColor Green
Write-Host "Échoués: $failedTests" -ForegroundColor $(if ($failedTests -eq 0) { "Green" } else { "Red" })

Write-Host "`nDétails par service:" -ForegroundColor Cyan
$testResults | Group-Object Service | ForEach-Object {
    $serviceName = $_.Name
    $serviceTests = $_.Group
    $servicePassed = ($serviceTests | Where-Object { $_.Passed -eq $true }).Count
    $serviceTotal = $serviceTests.Count
    Write-Host "  $serviceName : $servicePassed/$serviceTotal" -ForegroundColor $(if ($servicePassed -eq $serviceTotal) { "Green" } else { "Yellow" })
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Note: Pour tester avec un token valide depuis Keycloak," -ForegroundColor Yellow
Write-Host "utilisez le script test-authentification.ps1" -ForegroundColor Yellow
Write-Host "après avoir configuré les credentials Keycloak." -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
