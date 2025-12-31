# Script complet de test pour tous les services (version simplifiee)
# Verifie Eureka, teste les evenements et toutes les APIs

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TEST COMPLET DES SERVICES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$eurekaUrl = "http://localhost:8761"
$paymentServiceUrl = "http://localhost:8080"
$cryptoServiceUrl = "http://localhost:8082"
$auditServiceUrl = "http://localhost:8083"

$allTestsPassed = $true
$failedTests = @()

# Fonction pour attendre le traitement
function Wait-ForProcessing {
    param($seconds = 3)
    Write-Host "Attente de $seconds secondes pour le traitement..." -ForegroundColor Yellow
    Start-Sleep -Seconds $seconds
}

# ============================================
# 1. VERIFICATION EUREKA SERVER
# ============================================
Write-Host "1. VERIFICATION EUREKA SERVER" -ForegroundColor Yellow
Write-Host "Verification des services enregistres..." -ForegroundColor White

try {
    $eurekaResponse = Invoke-RestMethod -Uri "$eurekaUrl/eureka/apps" -Method GET -Headers @{"Accept"="application/json"} -ErrorAction SilentlyContinue
    
    if ($eurekaResponse -and $eurekaResponse.applications -and $eurekaResponse.applications.application) {
        $services = $eurekaResponse.applications.application
        Write-Host ""
        Write-Host "Services enregistres dans Eureka:" -ForegroundColor Green
        foreach ($app in $services) {
            $instanceCount = if ($app.instance) { 
                if ($app.instance -is [array]) { $app.instance.Count } else { 1 }
            } else { 0 }
            Write-Host "  [OK] $($app.name): $instanceCount instance(s)" -ForegroundColor Green
        }
        
        $expectedServices = @("PAYMENT-SERVICE", "CRYPTO-SERVICE", "AUDIT-SERVICE")
        $foundServices = @()
        foreach ($app in $services) {
            $foundServices += $app.name.ToUpper()
        }
        
        Write-Host ""
        Write-Host "Verification des services attendus:" -ForegroundColor Cyan
        foreach ($expected in $expectedServices) {
            if ($foundServices -contains $expected) {
                Write-Host "  [OK] $expected est enregistre" -ForegroundColor Green
            } else {
                Write-Host "  [ECHEC] $expected n'est PAS enregistre" -ForegroundColor Red
                $allTestsPassed = $false
                $failedTests += "Eureka: $expected manquant"
            }
        }
    } else {
        Write-Host "  [INFO] Verifiez manuellement le dashboard: $eurekaUrl" -ForegroundColor Yellow
    }
} catch {
    Write-Host "  [ECHEC] Erreur lors de la connexion a Eureka: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  [INFO] Verifiez que Eureka Server est demarre sur $eurekaUrl" -ForegroundColor Yellow
    $allTestsPassed = $false
    $failedTests += "Eureka: Connexion echouee"
}

Write-Host ""

# ============================================
# 2. TEST PAYMENT SERVICE
# ============================================
Write-Host "2. TEST PAYMENT SERVICE" -ForegroundColor Yellow

# Test 2.1: Creer un virement standard
Write-Host "2.1. Creation d'un virement standard..." -ForegroundColor White
$paymentRequest1 = @{
    sourceAccountId = "ACC123456"
    destinationIban = "FR1420041010050500013M02606"
    amount = 500.0
    type = "STANDARD"
} | ConvertTo-Json

try {
    $paymentResponse1 = Invoke-RestMethod -Uri "$paymentServiceUrl/api/v1/payments" `
        -Method POST `
        -ContentType "application/json" `
        -Body $paymentRequest1
    
    Write-Host "  [OK] Payment cree - ID: $($paymentResponse1.id), Status: $($paymentResponse1.status)" -ForegroundColor Green
    $paymentId1 = $paymentResponse1.id
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Payment Service: Creation standard"
    $paymentResponse1 = $null
}

Wait-ForProcessing 5

# Test 2.2: Creer un virement rejete
Write-Host "2.2. Creation d'un virement rejete (montant > 10000)..." -ForegroundColor White
$paymentRequest2 = @{
    sourceAccountId = "ACC123456"
    destinationIban = "FR1420041010050500013M02606"
    amount = 15000.0
    type = "STANDARD"
} | ConvertTo-Json

try {
    $paymentResponse2 = Invoke-RestMethod -Uri "$paymentServiceUrl/api/v1/payments" `
        -Method POST `
        -ContentType "application/json" `
        -Body $paymentRequest2
    
    Write-Host "  [OK] Payment cree (rejete) - ID: $($paymentResponse2.id), Status: $($paymentResponse2.status)" -ForegroundColor Green
    $paymentId2 = $paymentResponse2.id
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Payment Service: Creation rejetee"
    $paymentResponse2 = $null
}

Wait-ForProcessing 5

# ============================================
# 3. TEST CRYPTO SERVICE
# ============================================
Write-Host "3. TEST CRYPTO SERVICE" -ForegroundColor Yellow

# Test 3.1: Recuperer les prix
Write-Host "3.1. Recuperation des prix crypto..." -ForegroundColor White
try {
    $pricesResponse = Invoke-RestMethod -Uri "$cryptoServiceUrl/api/v1/crypto/prices" -Method GET
    Write-Host "  [OK] Prix crypto recuperes" -ForegroundColor Green
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Crypto Service: Recuperation prix"
}

# Test 3.2: Recuperer le wallet
Write-Host "3.2. Recuperation du wallet..." -ForegroundColor White
try {
    $walletResponse = Invoke-RestMethod -Uri "$cryptoServiceUrl/api/v1/crypto/wallet?userId=1" -Method GET
    Write-Host "  [OK] Wallet recupere" -ForegroundColor Green
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Crypto Service: Recuperation wallet"
}

# Test 3.3: Creer un trade BUY
Write-Host "3.3. Creation d'un trade BUY..." -ForegroundColor White
$cryptoRequest1 = @{
    symbol = "BTC"
    quantity = 0.1
    type = "BUY"
} | ConvertTo-Json

try {
    $cryptoResponse1 = Invoke-RestMethod -Uri "$cryptoServiceUrl/api/v1/crypto/trade?userId=1" `
        -Method POST `
        -ContentType "application/json" `
        -Body $cryptoRequest1
    
    Write-Host "  [OK] Trade BUY cree - ID: $($cryptoResponse1.id)" -ForegroundColor Green
    $cryptoId1 = $cryptoResponse1.id
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Crypto Service: Trade BUY"
    $cryptoResponse1 = $null
}

Wait-ForProcessing 5

# Test 3.4: Creer un trade SELL
Write-Host "3.4. Creation d'un trade SELL..." -ForegroundColor White
$cryptoRequest2 = @{
    symbol = "ETH"
    quantity = 0.5
    type = "SELL"
} | ConvertTo-Json

try {
    $cryptoResponse2 = Invoke-RestMethod -Uri "$cryptoServiceUrl/api/v1/crypto/trade?userId=1" `
        -Method POST `
        -ContentType "application/json" `
        -Body $cryptoRequest2
    
    Write-Host "  [OK] Trade SELL cree - ID: $($cryptoResponse2.id)" -ForegroundColor Green
    $cryptoId2 = $cryptoResponse2.id
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Crypto Service: Trade SELL"
    $cryptoResponse2 = $null
}

Wait-ForProcessing 5

# Test 3.5: Recuperer l'historique
Write-Host "3.5. Recuperation de l'historique crypto..." -ForegroundColor White
try {
    $historyResponse = Invoke-RestMethod -Uri "$cryptoServiceUrl/api/v1/crypto/history?userId=1" -Method GET
    Write-Host "  [OK] Historique crypto recupere - $($historyResponse.Count) transaction(s)" -ForegroundColor Green
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Crypto Service: Historique"
}

Wait-ForProcessing 5

# ============================================
# 4. VERIFICATION AUDIT SERVICE - EVENEMENTS
# ============================================
Write-Host "4. VERIFICATION DES EVENEMENTS D'AUDIT" -ForegroundColor Yellow
Write-Host "Verification que les evenements sont enregistres..." -ForegroundColor White

# Test 4.1: Verifier les evenements du Payment Service
Write-Host "4.1. Verification des evenements Payment Service..." -ForegroundColor White
try {
    $paymentAuditLogs = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/history?serviceName=payment-service`&size=20" -Method GET
    
    if ($paymentAuditLogs.auditLogs -and $paymentAuditLogs.auditLogs.Count -gt 0) {
        Write-Host "  [OK] $($paymentAuditLogs.totalElements) evenement(s) trouve(s) pour Payment Service" -ForegroundColor Green
        Write-Host "  Derniers evenements:" -ForegroundColor Cyan
        foreach ($log in $paymentAuditLogs.auditLogs | Select-Object -First 5) {
            Write-Host "    - $($log.actionType): $($log.status) (ID: $($log.id))" -ForegroundColor Gray
        }
    } else {
        Write-Host "  [ECHEC] Aucun evenement trouve pour Payment Service" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "Audit: Evenements Payment Service manquants"
    }
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit: Verification Payment Service"
}

# Test 4.2: Verifier les evenements du Crypto Service
Write-Host "4.2. Verification des evenements Crypto Service..." -ForegroundColor White
try {
    $cryptoAuditLogs = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/history?serviceName=crypto-service`&size=20" -Method GET
    
    if ($cryptoAuditLogs.auditLogs -and $cryptoAuditLogs.auditLogs.Count -gt 0) {
        Write-Host "  [OK] $($cryptoAuditLogs.totalElements) evenement(s) trouve(s) pour Crypto Service" -ForegroundColor Green
        Write-Host "  Derniers evenements:" -ForegroundColor Cyan
        foreach ($log in $cryptoAuditLogs.auditLogs | Select-Object -First 5) {
            Write-Host "    - $($log.actionType): $($log.status) (ID: $($log.id))" -ForegroundColor Gray
        }
    } else {
        Write-Host "  [ECHEC] Aucun evenement trouve pour Crypto Service" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "Audit: Evenements Crypto Service manquants"
    }
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit: Verification Crypto Service"
}

# ============================================
# 5. TEST COMPLET DES APIs AUDIT SERVICE
# ============================================
Write-Host "5. TEST COMPLET DES APIs AUDIT SERVICE" -ForegroundColor Yellow

# Test 5.1: Health Check
Write-Host "5.1. Health Check..." -ForegroundColor White
try {
    $healthResponse = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/health" -Method GET
    if ($healthResponse.status -eq "UP") {
        Write-Host "  [OK] Health check OK" -ForegroundColor Green
    } else {
        Write-Host "  [ECHEC] Health check echoue" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "Audit API: Health Check"
    }
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: Health Check"
}

# Test 5.2: POST /events
Write-Host "5.2. POST /events..." -ForegroundColor White
try {
    $eventBody = '{"userId":"test-user-001","actionType":"LOGIN","serviceName":"auth-service","description":"Connexion utilisateur","status":"SUCCESS"}'
    $eventResponse = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/events" `
        -Method POST `
        -Body $eventBody `
        -ContentType "application/json"
    Write-Host "  [OK] Evenement journalise - ID: $($eventResponse.auditLogId)" -ForegroundColor Green
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: POST /events"
}

# Test 5.3: POST /events/external
Write-Host "5.3. POST /events/external..." -ForegroundColor White
try {
    $externalEventBody = '{"userId":"test-user-002","actionType":"VIREMENT","serviceName":"payment-service","description":"Virement externe","status":"SUCCESS"}'
    $externalEventResponse = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/events/external" `
        -Method POST `
        -Body $externalEventBody `
        -ContentType "application/json"
    Write-Host "  [OK] Evenement externe recu - ID: $($externalEventResponse.auditLogId)" -ForegroundColor Green
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: POST /events/external"
}

# Test 5.4: GET /users/{userId}/history
Write-Host "5.4. GET /users/{userId}/history..." -ForegroundColor White
try {
    $userHistory = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/users/test-user-001/history" -Method GET
    Write-Host "  [OK] Historique utilisateur - Total: $($userHistory.totalElements)" -ForegroundColor Green
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /users/{userId}/history"
}

# Test 5.5: GET /history (global)
Write-Host "5.5. GET /history (global)..." -ForegroundColor White
try {
    $globalHistory = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/history?page=0`&size=10" -Method GET
    Write-Host "  [OK] Historique global - Total: $($globalHistory.totalElements)" -ForegroundColor Green
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /history"
}

# Test 5.6: GET /errors
Write-Host "5.6. GET /errors..." -ForegroundColor White
try {
    $errors = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/errors?page=0`&size=10" -Method GET
    Write-Host "  [OK] Erreurs recuperees - Total: $($errors.totalElements)" -ForegroundColor Green
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /errors"
}

# Test 5.7: GET /stats/user/{userId}
Write-Host "5.7. GET /stats/user/{userId}..." -ForegroundColor White
try {
    $userStats = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/stats/user/test-user-001" -Method GET
    Write-Host "  [OK] Statistiques utilisateur - Actions: $($userStats.totalActions)" -ForegroundColor Green
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /stats/user/{userId}"
}

# Test 5.8: GET /stats/errors
Write-Host "5.8. GET /stats/errors..." -ForegroundColor White
try {
    $errorStats = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/stats/errors" -Method GET
    Write-Host "  [OK] Statistiques erreurs - Total: $($errorStats.totalErrors)" -ForegroundColor Green
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /stats/errors"
}

# Test 5.9: GET /history avec filtres
Write-Host "5.9. GET /history avec filtres..." -ForegroundColor White
try {
    $filteredHistory = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/history?serviceName=payment-service`&status=SUCCESS`&size=5" -Method GET
    Write-Host "  [OK] Historique filtre - Total: $($filteredHistory.totalElements)" -ForegroundColor Green
} catch {
    Write-Host "  [ECHEC] Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /history (filtres)"
}

# ============================================
# RESUME FINAL
# ============================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RESUME DU TEST" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($allTestsPassed) {
    Write-Host "[OK] TOUS LES TESTS ONT REUSSI!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Services verifies:" -ForegroundColor Cyan
    Write-Host "  [OK] Eureka Server: $eurekaUrl" -ForegroundColor Green
    Write-Host "  [OK] Payment Service: $paymentServiceUrl" -ForegroundColor Green
    Write-Host "  [OK] Crypto Service: $cryptoServiceUrl" -ForegroundColor Green
    Write-Host "  [OK] Audit Service: $auditServiceUrl" -ForegroundColor Green
    Write-Host ""
    Write-Host "Evenements verifies:" -ForegroundColor Cyan
    Write-Host "  [OK] Les evenements Payment Service sont enregistres dans Audit" -ForegroundColor Green
    Write-Host "  [OK] Les evenements Crypto Service sont enregistres dans Audit" -ForegroundColor Green
} else {
    Write-Host "[ECHEC] CERTAINS TESTS ONT ECHOUE" -ForegroundColor Red
    Write-Host ""
    Write-Host "Tests echoues:" -ForegroundColor Yellow
    foreach ($test in $failedTests) {
        Write-Host "  - $test" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
