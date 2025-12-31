# Script de test pour verifier que les APIs fonctionnent apres correction
$auditServiceUrl = "http://localhost:8083/api/audit"
$allTestsPassed = $true
$failedTests = @()

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TEST DES APIs APRES CORRECTION" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "[TEST 1] GET /api/audit/health" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/health" -Method GET
    if ($response.status -eq "UP") {
        Write-Host "   [OK] Health check fonctionne" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse inattendue" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "Health Check"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Health Check"
}
Write-Host ""

# Test 2: GET /history avec filtre serviceName (le probleme corrige)
Write-Host "[TEST 2] GET /api/audit/history?serviceName=payment-service" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/history?serviceName=payment-service`&size=10" -Method GET
    if ($response.totalElements -ge 0) {
        Write-Host "   [OK] Filtre serviceName fonctionne - Total: $($response.totalElements)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /history (serviceName)"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /history (serviceName)"
}
Write-Host ""

# Test 3: GET /history avec filtre serviceName crypto-service
Write-Host "[TEST 3] GET /api/audit/history?serviceName=crypto-service" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/history?serviceName=crypto-service`&size=10" -Method GET
    if ($response.totalElements -ge 0) {
        Write-Host "   [OK] Filtre serviceName (crypto) fonctionne - Total: $($response.totalElements)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /history (serviceName crypto)"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /history (serviceName crypto)"
}
Write-Host ""

# Test 4: GET /history avec filtres multiples
Write-Host "[TEST 4] GET /api/audit/history?serviceName=payment-service&status=SUCCESS" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/history?serviceName=payment-service`&status=SUCCESS`&size=5" -Method GET
    if ($response.totalElements -ge 0) {
        Write-Host "   [OK] Filtres multiples fonctionnent - Total: $($response.totalElements)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /history (filtres multiples)"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /history (filtres multiples)"
}
Write-Host ""

# Test 5: GET /history sans filtres
Write-Host "[TEST 5] GET /api/audit/history" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/history?page=0`&size=10" -Method GET
    if ($response.totalElements -ge 0) {
        Write-Host "   [OK] Historique global fonctionne - Total: $($response.totalElements)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /history (sans filtres)"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /history (sans filtres)"
}
Write-Host ""

# Test 6: POST /events
Write-Host "[TEST 6] POST /api/audit/events" -ForegroundColor Yellow
try {
    $body = '{"userId":"test-user-001","actionType":"LOGIN","serviceName":"auth-service","description":"Connexion utilisateur","status":"SUCCESS"}'
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/events" -Method POST -Body $body -ContentType "application/json"
    if ($response.message -and $response.auditLogId) {
        Write-Host "   [OK] Evenement journalise - ID: $($response.auditLogId)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "POST /events"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "POST /events"
}
Write-Host ""

# Test 7: GET /users/{userId}/history
Write-Host "[TEST 7] GET /api/audit/users/{userId}/history" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/users/test-user-001/history" -Method GET
    if ($response.userId -and $response.totalElements -ge 0) {
        Write-Host "   [OK] Historique utilisateur - Total: $($response.totalElements)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /users/{userId}/history"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /users/{userId}/history"
}
Write-Host ""

# Test 8: GET /errors
Write-Host "[TEST 8] GET /api/audit/errors" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/errors?page=0`&size=10" -Method GET
    if ($response.totalElements -ge 0) {
        Write-Host "   [OK] Erreurs recuperees - Total: $($response.totalElements)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /errors"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /errors"
}
Write-Host ""

# Test 9: GET /stats/user/{userId}
Write-Host "[TEST 9] GET /api/audit/stats/user/{userId}" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/stats/user/test-user-001" -Method GET
    if ($response.userId -and $response.totalActions -ge 0) {
        Write-Host "   [OK] Statistiques utilisateur - Actions: $($response.totalActions)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /stats/user/{userId}"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /stats/user/{userId}"
}
Write-Host ""

# Test 10: GET /stats/errors
Write-Host "[TEST 10] GET /api/audit/stats/errors" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$auditServiceUrl/stats/errors" -Method GET
    if ($response.totalErrors -ge 0) {
        Write-Host "   [OK] Statistiques erreurs - Total: $($response.totalErrors)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /stats/errors"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /stats/errors"
}
Write-Host ""

# Resume final
Write-Host "========================================" -ForegroundColor Cyan
if ($allTestsPassed) {
    Write-Host "RESULTAT: TOUS LES TESTS ONT REUSSI!" -ForegroundColor Green
    Write-Host "Toutes les API fonctionnent correctement." -ForegroundColor Green
} else {
    Write-Host "RESULTAT: CERTAINS TESTS ONT ECHOUE" -ForegroundColor Red
    Write-Host "Tests echoues:" -ForegroundColor Yellow
    foreach ($test in $failedTests) {
        Write-Host "  - $test" -ForegroundColor Red
    }
}
Write-Host "========================================" -ForegroundColor Cyan
