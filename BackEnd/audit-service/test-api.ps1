# Script de test des API Audit Service
$baseUrl = "http://localhost:8083/api/audit"

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Test des API Audit Service" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Test 1: Health Check
Write-Host "1. Test Health Check..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/health" -Method GET
    Write-Host "   Status: $($response.status)" -ForegroundColor Green
    Write-Host "   Service: $($response.service)" -ForegroundColor Green
    Write-Host "   ✓ Health check réussi" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# Test 2: Journaliser un événement (POST /events)
Write-Host "2. Test Journalisation d'événement..." -ForegroundColor Yellow
try {
    $eventBody = @{
        userId = "user123"
        actionType = "CRYPTO_BUY"
        serviceName = "crypto-service"
        description = "Achat de Bitcoin"
        status = "SUCCESS"
        details = '{\"amount\": 0.5, \"currency\": \"BTC\", \"price\": 45000}'
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/events" -Method POST -Body $eventBody -ContentType "application/json"
    Write-Host "   Message: $($response.message)" -ForegroundColor Green
    Write-Host "   AuditLogId: $($response.auditLogId)" -ForegroundColor Green
    Write-Host "   ✓ Événement journalisé avec succès" -ForegroundColor Green
    $auditLogId1 = $response.auditLogId
} catch {
    Write-Host "   ✗ Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# Test 3: Journaliser un événement externe (POST /events/external)
Write-Host "3. Test Événement externe..." -ForegroundColor Yellow
try {
    $externalEventBody = @{
        userId = "user456"
        actionType = "TRANSFER"
        serviceName = "payment-service"
        description = "Virement bancaire"
        status = "SUCCESS"
        details = '{\"amount\": 1000, \"currency\": \"EUR\", \"recipient\": \"user789\"}'
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/events/external" -Method POST -Body $externalEventBody -ContentType "application/json"
    Write-Host "   Message: $($response.message)" -ForegroundColor Green
    Write-Host "   AuditLogId: $($response.auditLogId)" -ForegroundColor Green
    Write-Host "   ✓ Événement externe reçu avec succès" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# Test 4: Journaliser une erreur
Write-Host "4. Test Journalisation d'erreur..." -ForegroundColor Yellow
try {
    $errorBody = @{
        userId = "user123"
        actionType = "CRYPTO_SELL"
        serviceName = "crypto-service"
        description = "Tentative de vente"
        status = "FAILURE"
        errorMessage = "Solde insuffisant"
        details = '{\"requested\": 1.0, \"available\": 0.5}'
    } | ConvertTo-Json

    $response = Invoke-RestMethod -Uri "$baseUrl/events" -Method POST -Body $errorBody -ContentType "application/json"
    Write-Host "   Message: $($response.message)" -ForegroundColor Green
    Write-Host "   ✓ Erreur journalisée avec succès" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# Test 5: Historique par utilisateur (GET /users/{userId}/history)
Write-Host "5. Test Historique utilisateur..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/users/user123/history?page=0&size=10" -Method GET
    Write-Host "   UserId: $($response.userId)" -ForegroundColor Green
    Write-Host "   Total Elements: $($response.totalElements)" -ForegroundColor Green
    Write-Host "   Total Pages: $($response.totalPages)" -ForegroundColor Green
    Write-Host "   Nombre d'événements: $($response.auditLogs.Count)" -ForegroundColor Green
    if ($response.auditLogs.Count -gt 0) {
        Write-Host "   Dernier événement: $($response.auditLogs[0].actionType) - $($response.auditLogs[0].status)" -ForegroundColor Cyan
    }
    Write-Host "   ✓ Historique utilisateur récupéré" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# Test 6: Historique global (GET /history)
Write-Host "6. Test Historique global (Admin)..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/history?page=0`&size=10" -Method GET
    Write-Host "   Total Elements: $($response.totalElements)" -ForegroundColor Green
    Write-Host "   Total Pages: $($response.totalPages)" -ForegroundColor Green
    Write-Host "   Nombre d'événements: $($response.auditLogs.Count)" -ForegroundColor Green
    Write-Host "   ✓ Historique global récupéré" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# Test 7: Erreurs et échecs (GET /errors)
Write-Host "7. Test Traçabilité des erreurs..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/errors?page=0&size=10" -Method GET
    Write-Host "   Total Errors: $($response.totalElements)" -ForegroundColor Green
    Write-Host "   Nombre d'erreurs: $($response.auditLogs.Count)" -ForegroundColor Green
    if ($response.auditLogs.Count -gt 0) {
        Write-Host "   Dernière erreur: $($response.auditLogs[0].actionType) - $($response.auditLogs[0].status)" -ForegroundColor Cyan
        Write-Host "   Message: $($response.auditLogs[0].errorMessage)" -ForegroundColor Cyan
    }
    Write-Host "   ✓ Erreurs récupérées" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# Test 8: Statistiques utilisateur (GET /stats/user/{userId})
Write-Host "8. Test Statistiques utilisateur..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/stats/user/user123" -Method GET
    Write-Host "   UserId: $($response.userId)" -ForegroundColor Green
    Write-Host "   Total Actions: $($response.totalActions)" -ForegroundColor Green
    Write-Host "   ✓ Statistiques utilisateur récupérées" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# Test 9: Statistiques d'erreurs (GET /stats/errors)
Write-Host "9. Test Statistiques d'erreurs..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/stats/errors" -Method GET
    Write-Host "   Total Errors: $($response.totalErrors)" -ForegroundColor Green
    Write-Host "   ✓ Statistiques d'erreurs récupérées" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Erreur: $_" -ForegroundColor Red
}
Write-Host ""

# Test 10: Historique avec filtres
Write-Host "10. Test Historique avec filtres..." -ForegroundColor Yellow
try {
    $response = Invoke-RestMethod -Uri "$baseUrl/users/user123/history?page=0`&size=5`&actionType=CRYPTO_BUY`&status=SUCCESS" -Method GET
    Write-Host "   Total Elements filtrés: $($response.totalElements)" -ForegroundColor Green
    Write-Host "   ✓ Historique filtré récupéré" -ForegroundColor Green
} catch {
    Write-Host "   ✗ Erreur: $_" -ForegroundColor Red
}
Write-Host ""

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "Tests terminés!" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

