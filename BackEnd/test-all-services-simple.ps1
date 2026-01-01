# Script de test simple pour tous les services avec la configuration JWT
# Basé sur la configuration simple comme account-service

Write-Host "========================================" -ForegroundColor Magenta
Write-Host "Test Simple - Tous les Services JWT" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

$results = @()

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
        
        Write-Host "[$serviceName] $method $url" -ForegroundColor Cyan
        Write-Host "  Status: $status | Attendu: $expectedStatus | $(if ($success) { '✓' } else { '✗' })" -ForegroundColor $(if ($success) { "Green" } else { "Red" })
        
        return @{ Success = $success; Status = $status; Expected = $expectedStatus; Service = $serviceName; URL = $url }
    } catch {
        $status = $_.Exception.Response.StatusCode.value__
        if (-not $status) { $status = 500 }
        $success = ($status -eq $expectedStatus)
        
        Write-Host "[$serviceName] $method $url" -ForegroundColor Cyan
        Write-Host "  Status: $status | Attendu: $expectedStatus | $(if ($success) { '✓' } else { '✗' })" -ForegroundColor $(if ($success) { "Green" } else { "Red" })
        
        return @{ Success = $success; Status = $status; Expected = $expectedStatus; Service = $serviceName; URL = $url }
    }
}

Write-Host "=== ACCOUNT SERVICE (Port 8087) ===" -ForegroundColor Green
$results += Test-Endpoint -serviceName "ACCOUNT" -url "http://localhost:8087/api/accounts" -method "GET" -expectedStatus 401
$results += Test-Endpoint -serviceName "ACCOUNT" -url "http://localhost:8087/api/accounts/test/balance" -method "GET" -expectedStatus 401

Write-Host ""
Write-Host "=== PAYMENT SERVICE (Port 8086) ===" -ForegroundColor Green
$results += Test-Endpoint -serviceName "PAYMENT" -url "http://localhost:8086/api/v1/payments" -method "GET" -expectedStatus 401
$results += Test-Endpoint -serviceName "PAYMENT" -url "http://localhost:8086/api/v1/payments" -method "GET" -token "invalid-token" -expectedStatus 401

Write-Host ""
Write-Host "=== CRYPTO SERVICE (Port 8085) ===" -ForegroundColor Green
$results += Test-Endpoint -serviceName "CRYPTO" -url "http://localhost:8085/api/v1/crypto/prices" -method "GET" -expectedStatus 401
$results += Test-Endpoint -serviceName "CRYPTO" -url "http://localhost:8085/api/v1/crypto/wallet" -method "GET" -expectedStatus 401
$results += Test-Endpoint -serviceName "CRYPTO" -url "http://localhost:8085/api/v1/crypto/prices" -method "GET" -token "invalid-token" -expectedStatus 401

Write-Host ""
Write-Host "=== AUDIT SERVICE (Port 8084) ===" -ForegroundColor Green

# Endpoints internes (doivent fonctionner sans token)
$results += Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/log" -method "POST" -body @{serviceName="test"; action="test"; userId="1"} -expectedStatus 200
$results += Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/events" -method "POST" -body @{serviceName="test"; action="test"; userId="1"} -expectedStatus 201

# Endpoints utilisateurs sans token (doivent retourner 401)
$results += Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/users/1/history" -method "GET" -expectedStatus 401
$results += Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/history" -method "GET" -expectedStatus 401
$results += Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/errors" -method "GET" -expectedStatus 401
$results += Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/health" -method "GET" -expectedStatus 401
$results += Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/stats/user/1" -method "GET" -expectedStatus 401

# Endpoints avec token invalide (doivent retourner 401)
$results += Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/users/1/history" -method "GET" -token "invalid-token" -expectedStatus 401

# Endpoint interne avec token invalide (doit toujours fonctionner)
$results += Test-Endpoint -serviceName "AUDIT" -url "http://localhost:8084/api/audit/log" -method "POST" -token "invalid-token" -body @{serviceName="test"; action="test"; userId="1"} -expectedStatus 200

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "Résumé des Tests" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta

$total = $results.Count
$success = ($results | Where-Object { $_.Success }).Count
$failed = $total - $success

Write-Host "Total: $total tests" -ForegroundColor Cyan
Write-Host "Réussis: $success" -ForegroundColor Green
Write-Host "Échoués: $failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })

if ($failed -gt 0) {
    Write-Host ""
    Write-Host "Détails des échecs:" -ForegroundColor Red
    $results | Where-Object { -not $_.Success } | ForEach-Object {
        Write-Host "  ✗ $($_.Service) - $($_.URL)" -ForegroundColor Red
        Write-Host "    Attendu: $($_.Expected) | Obtenu: $($_.Status)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "Tests terminés!" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
