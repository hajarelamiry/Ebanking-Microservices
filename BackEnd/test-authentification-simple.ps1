# Script de test simplifié pour l'authentification JWT/Keycloak
# Teste les APIs avec et sans token

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test d'Authentification JWT/Keycloak" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$paymentServiceUrl = "http://localhost:8086"
$cryptoServiceUrl = "http://localhost:8085"
$auditServiceUrl = "http://localhost:8084"

# Test 1: Payment Service sans token (doit échouer avec 401)
Write-Host "`n[TEST 1] Payment Service - POST /api/v1/payments (SANS TOKEN)" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$paymentServiceUrl/api/v1/payments" `
        -Method Post `
        -Body '{"sourceAccountId":"123","destinationAccountId":"456","amount":100.0,"type":"TRANSFER"}' `
        -ContentType "application/json" `
        -ErrorAction Stop
    Write-Host "  ❌ ÉCHEC: La requête a réussi alors qu'elle devrait échouer!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 401) {
        Write-Host "  ✅ SUCCESS: 401 Unauthorized (attendu)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Status: $statusCode (attendu 401)" -ForegroundColor Yellow
    }
}

# Test 2: Crypto Service sans token (doit échouer avec 401)
Write-Host "`n[TEST 2] Crypto Service - GET /api/v1/crypto/prices (SANS TOKEN)" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$cryptoServiceUrl/api/v1/crypto/prices" `
        -Method Get `
        -ErrorAction Stop
    Write-Host "  ❌ ÉCHEC: La requête a réussi alors qu'elle devrait échouer!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 401) {
        Write-Host "  ✅ SUCCESS: 401 Unauthorized (attendu)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Status: $statusCode (attendu 401)" -ForegroundColor Yellow
    }
}

# Test 3: Audit Service - Endpoint interne sans token (doit réussir)
Write-Host "`n[TEST 3] Audit Service - POST /api/audit/log (SANS TOKEN - INTERNE)" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/log" `
        -Method Post `
        -Body '{"serviceName":"payment-service","actionType":"PAYMENT_CREATED","userId":"1","status":"SUCCESS","message":"Test"}' `
        -ContentType "application/json" `
        -ErrorAction Stop
    Write-Host "  ✅ SUCCESS: Endpoint interne accessible sans token (attendu)" -ForegroundColor Green
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "  ❌ ÉCHEC: Status $statusCode (attendu 200)" -ForegroundColor Red
}

# Test 4: Audit Service - Endpoint utilisateur sans token (doit échouer avec 401)
Write-Host "`n[TEST 4] Audit Service - GET /api/audit/users/1/history (SANS TOKEN)" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/users/1/history" `
        -Method Get `
        -ErrorAction Stop
    Write-Host "  ❌ ÉCHEC: La requête a réussi alors qu'elle devrait échouer!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 401) {
        Write-Host "  ✅ SUCCESS: 401 Unauthorized (attendu)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Status: $statusCode (attendu 401)" -ForegroundColor Yellow
    }
}

# Test 5: Test avec un token invalide (doit échouer avec 401)
Write-Host "`n[TEST 5] Payment Service - POST /api/v1/payments (TOKEN INVALIDE)" -ForegroundColor Yellow
try {
    $headers = @{
        "Authorization" = "Bearer invalid-token-12345"
        "Content-Type" = "application/json"
    }
    $response = Invoke-RestMethod -Uri "$paymentServiceUrl/api/v1/payments" `
        -Method Post `
        -Headers $headers `
        -Body '{"sourceAccountId":"123","destinationAccountId":"456","amount":100.0,"type":"TRANSFER"}' `
        -ErrorAction Stop
    Write-Host "  ❌ ÉCHEC: La requête a réussi alors qu'elle devrait échouer!" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 401) {
        Write-Host "  ✅ SUCCESS: 401 Unauthorized (attendu)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Status: $statusCode (attendu 401)" -ForegroundColor Yellow
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Résumé des tests:" -ForegroundColor Cyan
Write-Host "  - Endpoints protégés doivent retourner 401 sans token" -ForegroundColor Gray
Write-Host "  - Endpoints internes doivent être accessibles sans token" -ForegroundColor Gray
Write-Host "  - Tokens invalides doivent être rejetés" -ForegroundColor Gray
Write-Host "`nPour tester avec un token valide:" -ForegroundColor Yellow
Write-Host "  1. Obtenez un token depuis Keycloak" -ForegroundColor Gray
Write-Host "  2. Utilisez-le dans le header: Authorization: Bearer <token>" -ForegroundColor Gray
Write-Host "  3. Exécutez: test-authentification.ps1" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
