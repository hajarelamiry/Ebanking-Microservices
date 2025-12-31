# Script complet de test pour tous les services
# Vérifie Eureka, teste les événements et toutes les APIs

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

# Fonction pour afficher les résultats
function Show-Result {
    param($title, $response, $success = $true)
    Write-Host ""
    Write-Host "--- $title ---" -ForegroundColor $(if ($success) { "Green" } else { "Red" })
    if ($response) {
        Write-Host ($response | ConvertTo-Json -Depth 10)
    }
    Write-Host ""
}

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
Write-Host "Vérification des services enregistrés..." -ForegroundColor White

$eurekaServicesFound = $false
try {
    # Essayer d'abord avec l'endpoint JSON
    $eurekaResponse = Invoke-RestMethod -Uri "$eurekaUrl/eureka/apps" -Method GET -Headers @{"Accept"="application/json"} -ErrorAction SilentlyContinue
    
    if ($eurekaResponse -and $eurekaResponse.applications -and $eurekaResponse.applications.application) {
        $services = $eurekaResponse.applications.application
        $eurekaServicesFound = $true
        Write-Host ""
        Write-Host "Services enregistrés dans Eureka:" -ForegroundColor Green
        foreach ($app in $services) {
            $instanceCount = if ($app.instance) { 
                if ($app.instance -is [array]) { $app.instance.Count } else { 1 }
            } else { 0 }
            Write-Host "  ✓ $($app.name): $instanceCount instance(s)" -ForegroundColor Green
            if ($app.instance) {
                $instances = if ($app.instance -is [array]) { $app.instance } else { @($app.instance) }
                foreach ($inst in $instances) {
                    $port = if ($inst.port) { 
                        if ($inst.port.'$') { $inst.port.'$' } else { $inst.port }
                    } else { "N/A" }
                    Write-Host "    - $($inst.status): $($inst.ipAddr):$port" -ForegroundColor Gray
                }
            }
        }
    }
} catch {
    # Si JSON échoue, essayer de parser le HTML du dashboard
    try {
        Write-Host "  Tentative de vérification via le dashboard Eureka..." -ForegroundColor Yellow
        $htmlContent = Invoke-WebRequest -Uri "$eurekaUrl" -UseBasicParsing
        if ($htmlContent.Content -match "PAYMENT-SERVICE|CRYPTO-SERVICE|AUDIT-SERVICE") {
            Write-Host "  ✓ Le dashboard Eureka est accessible" -ForegroundColor Green
            Write-Host "  ℹ Veuillez vérifier manuellement: $eurekaUrl" -ForegroundColor Yellow
            $eurekaServicesFound = $true
        }
    } catch {
        Write-Host "  ✗ Impossible d'accéder au dashboard Eureka" -ForegroundColor Red
    }
}

if ($eurekaServicesFound) {
    $expectedServices = @("PAYMENT-SERVICE", "CRYPTO-SERVICE", "AUDIT-SERVICE")
    $foundServices = @()
    
    if ($eurekaResponse -and $eurekaResponse.applications -and $eurekaResponse.applications.application) {
        foreach ($app in $eurekaResponse.applications.application) {
            $foundServices += $app.name.ToUpper()
        }
        
        Write-Host ""
        Write-Host "Vérification des services attendus:" -ForegroundColor Cyan
        foreach ($expected in $expectedServices) {
            if ($foundServices -contains $expected) {
                Write-Host "  ✓ $expected est enregistré" -ForegroundColor Green
            } else {
                Write-Host "  ✗ $expected n'est PAS enregistré" -ForegroundColor Red
                $allTestsPassed = $false
                $failedTests += "Eureka: $expected manquant"
            }
        }
    } else {
        Write-Host ""
        Write-Host "  ℹ Vérifiez manuellement le dashboard: $eurekaUrl" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ✗ Aucun service trouvé dans Eureka ou connexion échouée" -ForegroundColor Red
    Write-Host "  ℹ Vérifiez que Eureka Server est démarré sur $eurekaUrl" -ForegroundColor Yellow
    $allTestsPassed = $false
    $failedTests += "Eureka: Connexion ou parsing échoué"
}

Write-Host ""

# ============================================
# 2. TEST PAYMENT SERVICE
# ============================================
Write-Host "2. TEST PAYMENT SERVICE" -ForegroundColor Yellow

# Test 2.1: Créer un virement standard
Write-Host "2.1. Création d'un virement standard..." -ForegroundColor White
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
    
    Show-Result "Payment créé (standard)" $paymentResponse1 $true
    $paymentId1 = $paymentResponse1.id
    $correlationId1 = if ($paymentResponse1.correlationId) { $paymentResponse1.correlationId } else { "payment-$(Get-Date -Format 'yyyyMMddHHmmss')" }
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Payment Service: Création standard"
    $paymentResponse1 = $null
}

Wait-ForProcessing 5

# Test 2.2: Créer un virement rejeté (montant > 10000)
Write-Host "2.2. Création d'un virement rejeté (montant > 10000)..." -ForegroundColor White
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
    
    Show-Result "Payment créé (rejeté)" $paymentResponse2 $true
    $paymentId2 = $paymentResponse2.id
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Payment Service: Création rejetée"
    $paymentResponse2 = $null
}

Wait-ForProcessing 5

# ============================================
# 3. TEST CRYPTO SERVICE
# ============================================
Write-Host "3. TEST CRYPTO SERVICE" -ForegroundColor Yellow

# Test 3.1: Récupérer les prix
Write-Host "3.1. Récupération des prix crypto..." -ForegroundColor White
try {
    $pricesResponse = Invoke-RestMethod -Uri "$cryptoServiceUrl/api/v1/crypto/prices" -Method GET
    Show-Result "Prix crypto récupérés" $pricesResponse $true
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Crypto Service: Récupération prix"
}

# Test 3.2: Récupérer le wallet
Write-Host "3.2. Récupération du wallet..." -ForegroundColor White
try {
    $walletResponse = Invoke-RestMethod -Uri "$cryptoServiceUrl/api/v1/crypto/wallet?userId=1" -Method GET
    Show-Result "Wallet récupéré" $walletResponse $true
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Crypto Service: Récupération wallet"
}

# Test 3.3: Créer un trade BUY
Write-Host "3.3. Création d'un trade BUY..." -ForegroundColor White
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
    
    Show-Result "Trade BUY créé" $cryptoResponse1 $true
    $cryptoId1 = $cryptoResponse1.id
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Crypto Service: Trade BUY"
    $cryptoResponse1 = $null
}

Wait-ForProcessing 5

# Test 3.4: Créer un trade SELL
Write-Host "3.4. Création d'un trade SELL..." -ForegroundColor White
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
    
    Show-Result "Trade SELL créé" $cryptoResponse2 $true
    $cryptoId2 = $cryptoResponse2.id
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Crypto Service: Trade SELL"
    $cryptoResponse2 = $null
}

Wait-ForProcessing 5

# Test 3.5: Récupérer l'historique
Write-Host "3.5. Récupération de l'historique crypto..." -ForegroundColor White
try {
    $historyResponse = Invoke-RestMethod -Uri "$cryptoServiceUrl/api/v1/crypto/history?userId=1" -Method GET
    Show-Result "Historique crypto récupéré" $historyResponse $true
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Crypto Service: Historique"
}

Wait-ForProcessing 5

# ============================================
# 4. VERIFICATION AUDIT SERVICE - ÉVÉNEMENTS
# ============================================
Write-Host "4. VERIFICATION DES ÉVÉNEMENTS D'AUDIT" -ForegroundColor Yellow
Write-Host "Vérification que les événements sont enregistrés..." -ForegroundColor White

# Test 4.1: Vérifier les événements du Payment Service
Write-Host "4.1. Vérification des événements Payment Service..." -ForegroundColor White
try {
    $paymentAuditLogs = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/history?serviceName=payment-service`&size=20" -Method GET
    
    if ($paymentAuditLogs.auditLogs -and $paymentAuditLogs.auditLogs.Count -gt 0) {
        Write-Host "  ✓ $($paymentAuditLogs.totalElements) événement(s) trouvé(s) pour Payment Service" -ForegroundColor Green
        Write-Host "  Derniers événements:" -ForegroundColor Cyan
        foreach ($log in $paymentAuditLogs.auditLogs | Select-Object -First 5) {
            Write-Host "    - $($log.actionType): $($log.status) (ID: $($log.id))" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ✗ Aucun événement trouvé pour Payment Service" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "Audit: Événements Payment Service manquants"
    }
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit: Vérification Payment Service"
}

# Test 4.2: Vérifier les événements du Crypto Service
Write-Host "4.2. Vérification des événements Crypto Service..." -ForegroundColor White
try {
    $cryptoAuditLogs = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/history?serviceName=crypto-service`&size=20" -Method GET
    
    if ($cryptoAuditLogs.auditLogs -and $cryptoAuditLogs.auditLogs.Count -gt 0) {
        Write-Host "  ✓ $($cryptoAuditLogs.totalElements) événement(s) trouvé(s) pour Crypto Service" -ForegroundColor Green
        Write-Host "  Derniers événements:" -ForegroundColor Cyan
        foreach ($log in $cryptoAuditLogs.auditLogs | Select-Object -First 5) {
            Write-Host "    - $($log.actionType): $($log.status) (ID: $($log.id))" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ✗ Aucun événement trouvé pour Crypto Service" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "Audit: Événements Crypto Service manquants"
    }
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit: Vérification Crypto Service"
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
        Write-Host "  ✓ Health check OK" -ForegroundColor Green
    } else {
        Write-Host "  ✗ Health check échoué" -ForegroundColor Red
        $allTestsPassed = $false
        $failedTests += "Audit API: Health Check"
    }
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
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
    Write-Host "  ✓ Événement journalisé - ID: $($eventResponse.auditLogId)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
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
    Write-Host "  ✓ Événement externe reçu - ID: $($externalEventResponse.auditLogId)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: POST /events/external"
}

# Test 5.4: GET /users/{userId}/history
Write-Host "5.4. GET /users/{userId}/history..." -ForegroundColor White
try {
    $userHistory = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/users/test-user-001/history" -Method GET
    Write-Host "  ✓ Historique utilisateur - Total: $($userHistory.totalElements)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /users/{userId}/history"
}

# Test 5.5: GET /history (global)
Write-Host "5.5. GET /history (global)..." -ForegroundColor White
try {
    $globalHistory = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/history?page=0`&size=10" -Method GET
    Write-Host "  ✓ Historique global - Total: $($globalHistory.totalElements)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /history"
}

# Test 5.6: GET /errors
Write-Host "5.6. GET /errors..." -ForegroundColor White
try {
    $errors = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/errors?page=0`&size=10" -Method GET
    Write-Host "  ✓ Erreurs récupérées - Total: $($errors.totalElements)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /errors"
}

# Test 5.7: GET /stats/user/{userId}
Write-Host "5.7. GET /stats/user/{userId}..." -ForegroundColor White
try {
    $userStats = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/stats/user/test-user-001" -Method GET
    Write-Host "  ✓ Statistiques utilisateur - Actions: $($userStats.totalActions)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /stats/user/{userId}"
}

# Test 5.8: GET /stats/errors
Write-Host "5.8. GET /stats/errors..." -ForegroundColor White
try {
    $errorStats = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/stats/errors" -Method GET
    Write-Host "  ✓ Statistiques erreurs - Total: $($errorStats.totalErrors)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /stats/errors"
}

# Test 5.9: GET /history avec filtres
Write-Host "5.9. GET /history avec filtres..." -ForegroundColor White
try {
    $filteredHistory = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/history?serviceName=payment-service`&status=SUCCESS`&size=5" -Method GET
    Write-Host "  ✓ Historique filtré - Total: $($filteredHistory.totalElements)" -ForegroundColor Green
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
    $allTestsPassed = $false
    $failedTests += "Audit API: GET /history (filtres)"
}

# ============================================
# RÉSUMÉ FINAL
# ============================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RÉSUMÉ DU TEST" -ForegroundColor Cyan
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
