# Verification complete de tous les endpoints API
$baseUrl = "http://localhost:8083/api/audit"
$allTestsPassed = $true
$failedTests = @()

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "VERIFICATION COMPLETE DES API" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "[TEST 1] GET /api/audit/health" -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/health" -Method GET
    if ($response.status -eq "UP" -and $response.service -eq "audit-service") {
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

# Test 2: POST /events
Write-Host "[TEST 2] POST /api/audit/events" -ForegroundColor Yellow
try {
    $body = '{"userId":"test-user-001","actionType":"LOGIN","serviceName":"auth-service","description":"Connexion utilisateur","status":"SUCCESS"}'
    $response = Invoke-RestMethod -Uri "$baseUrl/events" -Method POST -Body $body -ContentType "application/json"
    if ($response.message -and $response.auditLogId) {
        Write-Host "   [OK] Evenement journalise - ID: $($response.auditLogId)" -ForegroundColor Green
        $eventId1 = $response.auditLogId
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

# Test 3: POST /events/external
Write-Host "[TEST 3] POST /api/audit/events/external" -ForegroundColor Yellow
try {
    $body = '{"userId":"test-user-002","actionType":"VIREMENT","serviceName":"payment-service","description":"Virement externe","status":"SUCCESS"}'
    $response = Invoke-RestMethod -Uri "$baseUrl/events/external" -Method POST -Body $body -ContentType "application/json"
    if ($response.message -and $response.auditLogId) {
        Write-Host "   [OK] Evenement externe recu - ID: $($response.auditLogId)" -ForegroundColor Green
        $eventId2 = $response.auditLogId
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "POST /events/external"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "POST /events/external"
}
Write-Host ""

# Test 4: POST /events avec erreur
Write-Host "[TEST 4] POST /api/audit/events (avec erreur)" -ForegroundColor Yellow
try {
    $body = '{"userId":"test-user-001","actionType":"CRYPTO_SELL","serviceName":"crypto-service","description":"Echec vente","status":"FAILURE","errorMessage":"Solde insuffisant"}'
    $response = Invoke-RestMethod -Uri "$baseUrl/events" -Method POST -Body $body -ContentType "application/json"
    if ($response.message -and $response.auditLogId) {
        Write-Host "   [OK] Erreur journalisee - ID: $($response.auditLogId)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "POST /events (erreur)"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "POST /events (erreur)"
}
Write-Host ""

# Test 5: GET /users/{userId}/history
Write-Host "[TEST 5] GET /api/audit/users/{userId}/history" -ForegroundColor Yellow
try {
    $url = "$baseUrl/users/test-user-001/history"
    $response = Invoke-RestMethod -Uri $url -Method GET
    if ($response.userId -and $response.totalElements -ge 0) {
        Write-Host "   [OK] Historique recupere - UserId: $($response.userId), Total: $($response.totalElements)" -ForegroundColor Green
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

# Test 6: GET /users/{userId}/history avec pagination
Write-Host "[TEST 6] GET /api/audit/users/{userId}/history?page=0&size=5" -ForegroundColor Yellow
try {
    $url = "$baseUrl/users/test-user-001/history?page=0&size=5"
    $response = Invoke-RestMethod -Uri $url -Method GET
    if ($response.totalPages -ge 0) {
        Write-Host "   [OK] Pagination fonctionne - Page: $($response.currentPage), Size: $($response.auditLogs.Count)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /users/{userId}/history (pagination)"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /users/{userId}/history (pagination)"
}
Write-Host ""

# Test 7: GET /history (admin)
Write-Host "[TEST 7] GET /api/audit/history" -ForegroundColor Yellow
try {
    $url = "$baseUrl/history?page=0&size=10"
    $response = Invoke-RestMethod -Uri $url -Method GET
    if ($response.totalElements -ge 0) {
        Write-Host "   [OK] Historique global recupere - Total: $($response.totalElements)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /history"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /history"
}
Write-Host ""

# Test 8: GET /errors
Write-Host "[TEST 8] GET /api/audit/errors" -ForegroundColor Yellow
try {
    $url = "$baseUrl/errors?page=0&size=10"
    $response = Invoke-RestMethod -Uri $url -Method GET
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
    $response = Invoke-RestMethod -Uri "$baseUrl/stats/user/test-user-001" -Method GET
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
    $response = Invoke-RestMethod -Uri "$baseUrl/stats/errors" -Method GET
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

# Test 11: GET /users/{userId}/history avec filtre actionType
Write-Host "[TEST 11] GET /api/audit/users/{userId}/history?actionType=LOGIN" -ForegroundColor Yellow
try {
    $url = "$baseUrl/users/test-user-001/history?actionType=LOGIN"
    $response = Invoke-RestMethod -Uri $url -Method GET
    if ($response.totalElements -ge 0) {
        Write-Host "   [OK] Filtre actionType fonctionne - Total: $($response.totalElements)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /users/{userId}/history (filtre actionType)"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /users/{userId}/history (filtre actionType)"
}
Write-Host ""

# Test 12: GET /users/{userId}/history avec filtre status
Write-Host "[TEST 12] GET /api/audit/users/{userId}/history?status=SUCCESS" -ForegroundColor Yellow
try {
    $url = "$baseUrl/users/test-user-001/history?status=SUCCESS"
    $response = Invoke-RestMethod -Uri $url -Method GET
    if ($response.totalElements -ge 0) {
        Write-Host "   [OK] Filtre status fonctionne - Total: $($response.totalElements)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /users/{userId}/history (filtre status)"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /users/{userId}/history (filtre status)"
}
Write-Host ""

# Test 13: GET /history avec filtres multiples
Write-Host "[TEST 13] GET /api/audit/history?serviceName=crypto-service" -ForegroundColor Yellow
try {
    $url = "$baseUrl/history?serviceName=crypto-service"
    $response = Invoke-RestMethod -Uri $url -Method GET
    if ($response.totalElements -ge 0) {
        Write-Host "   [OK] Filtre serviceName fonctionne - Total: $($response.totalElements)" -ForegroundColor Green
    } else {
        Write-Host "   [ECHEC] Reponse incomplete" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "GET /history (filtre serviceName)"
    }
} catch {
    Write-Host "   [ECHEC] $_" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "GET /history (filtre serviceName)"
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

