# Script de test spécifique pour l'audit service - Utilisation des tokens JWT
# Teste tous les endpoints de l'audit service avec différents scénarios

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test Audit Service - Utilisation des Tokens JWT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$auditServiceUrl = "http://localhost:8084"

$script:testResults = @()

# Fonction pour tester un endpoint
function Test-AuditEndpoint {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Token = $null,
        [string]$Body = $null,
        [string]$ExpectedStatus,
        [string]$Description
    )
    
    Write-Host "`n[TEST] $Description" -ForegroundColor Yellow
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
        Write-Host "  Status: $statusCode (SUCCESS)" -ForegroundColor Green
        
        $result = [PSCustomObject]@{
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
            # Afficher le body de l'erreur si disponible
            if ($_.Exception.Response) {
                try {
                    $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                    $responseBody = $reader.ReadToEnd()
                    if ($responseBody) {
                        Write-Host "  Response Body: $responseBody" -ForegroundColor Gray
                    }
                } catch {
                    # Ignorer les erreurs de lecture
                }
            }
        }
        
        $result = [PSCustomObject]@{
            Test = $Description
            Status = if ($isExpected) { "PASS" } else { "FAIL" }
            Expected = $ExpectedStatus
            Actual = $statusCode
        }
        $script:testResults += $result
        return $isExpected
    }
}

Write-Host "=== PHASE 1: Endpoints INTERNES (doivent être accessibles SANS token) ===" -ForegroundColor Cyan

Test-AuditEndpoint -Method "POST" `
    -Url "$auditServiceUrl/api/audit/log" `
    -Body '{"serviceName":"payment-service","actionType":"PAYMENT_CREATED","userId":"1","status":"SUCCESS","message":"Test interne"}' `
    -ExpectedStatus "200" `
    -Description "POST /api/audit/log (INTERNE - sans token)"

Test-AuditEndpoint -Method "POST" `
    -Url "$auditServiceUrl/api/audit/events" `
    -Body '{"serviceName":"crypto-service","actionType":"CRYPTO_TRADE","userId":"1","status":"SUCCESS","message":"Test interne"}' `
    -ExpectedStatus "200" `
    -Description "POST /api/audit/events (INTERNE - sans token)"

Test-AuditEndpoint -Method "POST" `
    -Url "$auditServiceUrl/api/audit/events/external" `
    -Body '{"serviceName":"test-service","actionType":"TEST","userId":"1","status":"SUCCESS","message":"Test externe"}' `
    -ExpectedStatus "200" `
    -Description "POST /api/audit/events/external (INTERNE - sans token)"

Write-Host "`n=== PHASE 2: Endpoints UTILISATEURS (doivent retourner 401 SANS token) ===" -ForegroundColor Cyan

Test-AuditEndpoint -Method "GET" `
    -Url "$auditServiceUrl/api/audit/users/1/history" `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/users/1/history (SANS TOKEN - doit être 401)"

Test-AuditEndpoint -Method "GET" `
    -Url "$auditServiceUrl/api/audit/users/1/history?page=0&size=10" `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/users/1/history?page=0&size=10 (SANS TOKEN - doit être 401)"

Test-AuditEndpoint -Method "GET" `
    -Url "$auditServiceUrl/api/audit/history" `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/history (SANS TOKEN - ADMIN uniquement, doit être 401)"

Test-AuditEndpoint -Method "GET" `
    -Url "$auditServiceUrl/api/audit/errors" `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/errors (SANS TOKEN - AGENT/ADMIN, doit être 401)"

Test-AuditEndpoint -Method "GET" `
    -Url "$auditServiceUrl/api/audit/health" `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/health (SANS TOKEN - doit être 401)"

Test-AuditEndpoint -Method "GET" `
    -Url "$auditServiceUrl/api/audit/stats/user/1" `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/stats/user/1 (SANS TOKEN - doit être 401)"

Test-AuditEndpoint -Method "GET" `
    -Url "$auditServiceUrl/api/audit/stats/errors" `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/stats/errors (SANS TOKEN - ADMIN, doit être 401)"

Write-Host "`n=== PHASE 3: Endpoints UTILISATEURS avec TOKEN INVALIDE (doivent retourner 401) ===" -ForegroundColor Cyan

$invalidToken = "invalid-token-12345-abcdef"

Test-AuditEndpoint -Method "GET" `
    -Url "$auditServiceUrl/api/audit/users/1/history" `
    -Token $invalidToken `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/users/1/history (TOKEN INVALIDE - doit être 401)"

Test-AuditEndpoint -Method "GET" `
    -Url "$auditServiceUrl/api/audit/history" `
    -Token $invalidToken `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/history (TOKEN INVALIDE - doit être 401)"

Test-AuditEndpoint -Method "GET" `
    -Url "$auditServiceUrl/api/audit/errors" `
    -Token $invalidToken `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/errors (TOKEN INVALIDE - doit être 401)"

Test-AuditEndpoint -Method "GET" `
    -Url "$auditServiceUrl/api/audit/health" `
    -Token $invalidToken `
    -ExpectedStatus "401" `
    -Description "GET /api/audit/health (TOKEN INVALIDE - doit être 401)"

Write-Host "`n=== PHASE 4: Endpoints INTERNES avec TOKEN (doivent toujours fonctionner) ===" -ForegroundColor Cyan

Test-AuditEndpoint -Method "POST" `
    -Url "$auditServiceUrl/api/audit/log" `
    -Token $invalidToken `
    -Body '{"serviceName":"payment-service","actionType":"PAYMENT_CREATED","userId":"1","status":"SUCCESS","message":"Test avec token"}' `
    -ExpectedStatus "200" `
    -Description "POST /api/audit/log (INTERNE - avec token, doit toujours fonctionner)"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Résumé des Tests - Audit Service" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

$passed = ($script:testResults | Where-Object { $_.Status -eq "PASS" }).Count
$failed = ($script:testResults | Where-Object { $_.Status -eq "FAIL" }).Count
$total = $script:testResults.Count

Write-Host "Total: $total tests" -ForegroundColor White
Write-Host "Réussis: $passed" -ForegroundColor Green
Write-Host "Échoués: $failed" -ForegroundColor $(if ($failed -gt 0) { "Red" } else { "Green" })

if ($failed -gt 0) {
    Write-Host "`nDétails des échecs:" -ForegroundColor Yellow
    $script:testResults | Where-Object { $_.Status -eq "FAIL" } | ForEach-Object {
        Write-Host "  ❌ $($_.Test)" -ForegroundColor Red
        Write-Host "     Attendu: $($_.Expected) | Obtenu: $($_.Actual)" -ForegroundColor Gray
    }
} else {
    Write-Host "`n✅ Tous les tests sont passés avec succès!" -ForegroundColor Green
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Tests terminés!" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
