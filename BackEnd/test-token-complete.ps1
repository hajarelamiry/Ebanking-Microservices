# Script de test complet pour l'utilisation des tokens JWT
# Teste tous les services avec différents scénarios

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Complet - Utilisation des Tokens JWT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$paymentServiceUrl = "http://localhost:8086"
$cryptoServiceUrl = "http://localhost:8085"
$auditServiceUrl = "http://localhost:8084"

$script:testResults = @()

# Fonction pour tester un endpoint
function Test-Endpoint {
    param(
        [string]$ServiceName,
        [string]$Method,
        [string]$Url,
        [string]$Token = $null,
        [string]$Body = $null,
        [string]$ExpectedStatus,
        [string]$Description
    )
    
    Write-Host "`n[$ServiceName] $Description" -ForegroundColor Yellow
    Write-Host "  URL: $Url" -ForegroundColor Gray
    
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
        Write-Host "  Status: $statusCode (SUCCESS)" -ForegroundColor Green
        
        $result = [PSCustomObject]@{
            Service = $ServiceName
            Test = $Description
            Status = "PASS"
            Expected = $ExpectedStatus
            Actual = $statusCode
        }
        $script:testResults += $result
        return $true
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if (-not $statusCode) {
            $statusCode = "N/A"
        }
        
        $isExpected = ($statusCode -eq $ExpectedStatus)
        $color = if ($isExpected) { "Green" } else { "Red" }
        $icon = if ($isExpected) { "✅" } else { "❌" }
        
        Write-Host "  Status: $statusCode $icon" -ForegroundColor $color
        if (-not $isExpected) {
            Write-Host "  Attendu: $ExpectedStatus" -ForegroundColor Yellow
        }
        
        $result = [PSCustomObject]@{
            Service = $ServiceName
            Test = $Description
            Status = if ($isExpected) { "PASS" } else { "FAIL" }
            Expected = $ExpectedStatus
            Actual = $statusCode
        }
        $script:testResults += $result
        return $isExpected
    }
}

Write-Host "=== PHASE 1: Tests sans token (doivent retourner 401) ===" -ForegroundColor Cyan

# Payment Service
Test-Endpoint -ServiceName "Payment" -Method "POST" `
    -Url "$paymentServiceUrl/api/v1/payments" `
    -Body '{"sourceAccountId":"123","destinationAccountId":"456","amount":100.0,"type":"TRANSFER"}' `
    -ExpectedStatus "401" `
    -Description "POST /api/v1/payments (SANS TOKEN)"

Test-Endpoint -ServiceName "Payment" -Method "GET" `
    -Url "$paymentServiceUrl/api/v1/payments/1" `
    -ExpectedStatus "401" `
    -Description "GET /api/v1/payments/1 (SANS TOKEN)"

# Crypto Service
Test-Endpoint -ServiceName "Crypto" -Method "GET" `
    -Url "$cryptoServiceUrl/api/v1/crypto/prices" `
    -ExpectedStatus "401" `
    -Description "GET /api/v1/crypto/prices (SANS TOKEN)"

Test-Endpoint -ServiceName "Crypto" -Method "GET" `
    -Url "$cryptoServiceUrl/api/v1/crypto/wallet?userId=1" `
    -ExpectedStatus "401" `
    -Description "GET /api/v1/crypto/wallet (SANS TOKEN)"

Test-Endpoint -ServiceName "Crypto" -Method "POST" `
    -Url "$cryptoServiceUrl/api/v1/crypto/trade?userId=1" `
    -Body '{"symbol":"BTC","quantity":0.1,"type":"BUY"}' `
    -ExpectedStatus "401" `
    -Description "POST /api/v1/crypto/trade (SANS TOKEN)"

# Audit Service - Endpoints utilisateurs
Test-Endpoint -ServiceName "Audit" -Method "GET" `
    -Url "$auditServiceUrl/api/audit/users/1/history" `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/users/1/history (SANS TOKEN)"

Test-Endpoint -ServiceName "Audit" -Method "GET" `
    -Url "$auditServiceUrl/api/audit/history" `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/history (SANS TOKEN - ADMIN)"

Test-Endpoint -ServiceName "Audit" -Method "GET" `
    -Url "$auditServiceUrl/api/audit/errors" `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/errors (SANS TOKEN - AGENT/ADMIN)"

Test-Endpoint -ServiceName "Audit" -Method "GET" `
    -Url "$auditServiceUrl/api/audit/health" `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/health (SANS TOKEN)"

Write-Host "`n=== PHASE 2: Tests endpoints internes (doivent être accessibles) ===" -ForegroundColor Cyan

# Audit Service - Endpoints internes
Test-Endpoint -ServiceName "Audit" -Method "POST" `
    -Url "$auditServiceUrl/api/audit/log" `
    -Body '{"serviceName":"payment-service","actionType":"PAYMENT_CREATED","userId":"1","status":"SUCCESS","message":"Test"}' `
    -ExpectedStatus "200" `
    -Description "POST /api/audit/log (INTERNE - sans token)"

Test-Endpoint -ServiceName "Audit" -Method "POST" `
    -Url "$auditServiceUrl/api/audit/events" `
    -Body '{"serviceName":"test","actionType":"TEST","userId":"1","status":"SUCCESS","message":"Test"}' `
    -ExpectedStatus "200" `
    -Description "POST /api/audit/events (INTERNE - sans token)"

Write-Host "`n=== PHASE 3: Tests avec token invalide (doivent retourner 401) ===" -ForegroundColor Cyan

$invalidToken = "invalid-token-12345"

Test-Endpoint -ServiceName "Payment" -Method "POST" `
    -Url "$paymentServiceUrl/api/v1/payments" `
    -Token $invalidToken `
    -Body '{"sourceAccountId":"123","destinationAccountId":"456","amount":100.0,"type":"TRANSFER"}' `
    -ExpectedStatus "401" `
    -Description "POST /api/v1/payments (TOKEN INVALIDE)"

Test-Endpoint -ServiceName "Crypto" -Method "GET" `
    -Url "$cryptoServiceUrl/api/v1/crypto/prices" `
    -Token $invalidToken `
    -ExpectedStatus "401" `
    -Description "GET /api/v1/crypto/prices (TOKEN INVALIDE)"

Test-Endpoint -ServiceName "Audit" -Method "GET" `
    -Url "$auditServiceUrl/api/audit/users/1/history" `
    -Token $invalidToken `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/users/1/history (TOKEN INVALIDE)"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Résumé des Tests" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$passed = ($testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$total = $testResults.Count

Write-Host "Total: $total tests" -ForegroundColor White
Write-Host "Réussis: $passed" -ForegroundColor Green
Write-Host "Échoués: $failed" -ForegroundColor $(if ($failed -gt 0) { "Red" } else { "Green" })

Write-Host "`nDétails des échecs:" -ForegroundColor Yellow
$testResults | Where-Object { $_.Status -eq "FAIL" } | ForEach-Object {
    Write-Host "  [$($_.Service)] $($_.Test)" -ForegroundColor Red
    Write-Host "    Attendu: $($_.Expected) | Obtenu: $($_.Actual)" -ForegroundColor Gray
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Tests terminés!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
