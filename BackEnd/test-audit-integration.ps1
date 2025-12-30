# Script de test pour vérifier l'intégration Audit Service
# Teste les opérations Payment et Crypto, puis vérifie les logs dans Audit

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  TEST D'INTEGRATION AUDIT SERVICE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$paymentServiceUrl = "http://localhost:8080"
$cryptoServiceUrl = "http://localhost:8082"
$auditServiceUrl = "http://localhost:8083"

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

# Fonction pour attendre un peu
function Wait-ForProcessing {
    param($seconds = 2)
    Write-Host "Attente de $seconds secondes pour le traitement..." -ForegroundColor Yellow
    Start-Sleep -Seconds $seconds
}

# ============================================
# 1. TEST PAYMENT SERVICE
# ============================================
Write-Host "1. TEST PAYMENT SERVICE" -ForegroundColor Yellow
Write-Host "Création d'un virement..." -ForegroundColor White

$paymentRequest = @{
    sourceAccountId = "ACC123456"
    destinationIban = "FR1420041010050500013M02606"
    amount = 500.0
    type = "STANDARD"
} | ConvertTo-Json

try {
    $paymentResponse = Invoke-RestMethod -Uri "$paymentServiceUrl/api/v1/payments" `
        -Method POST `
        -ContentType "application/json" `
        -Body $paymentRequest
    
    Show-Result "Payment créé" $paymentResponse $true
    
    # Extraire le correlationId si disponible
    $correlationId = $paymentResponse.correlationId
    if (-not $correlationId) {
        $correlationId = "payment-$(Get-Date -Format 'yyyyMMddHHmmss')"
    }
    
} catch {
    Write-Host "ERREUR lors de la création du payment:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    $paymentResponse = $null
}

Wait-ForProcessing 3

# ============================================
# 2. TEST CRYPTO SERVICE
# ============================================
Write-Host "2. TEST CRYPTO SERVICE" -ForegroundColor Yellow
Write-Host "Création d'une transaction crypto..." -ForegroundColor White

$cryptoRequest = @{
    symbol = "BTC"
    quantity = 0.5
    type = "BUY"
} | ConvertTo-Json

try {
    $cryptoResponse = Invoke-RestMethod -Uri "$cryptoServiceUrl/api/v1/crypto/trade?userId=1" `
        -Method POST `
        -ContentType "application/json" `
        -Body $cryptoRequest
    
    Show-Result "Transaction Crypto créée" $cryptoResponse $true
    
    # Extraire le correlationId si disponible
    if (-not $correlationId) {
        $correlationId = "crypto-$(Get-Date -Format 'yyyyMMddHHmmss')"
    }
    
} catch {
    Write-Host "ERREUR lors de la création de la transaction crypto:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    $cryptoResponse = $null
}

Wait-ForProcessing 3

# ============================================
# 3. VERIFICATION AUDIT SERVICE
# ============================================
Write-Host "3. VERIFICATION AUDIT SERVICE" -ForegroundColor Yellow
Write-Host "Récupération de l'historique d'audit..." -ForegroundColor White

try {
    # Récupérer tous les logs d'audit
    $auditHistory = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/history?size=50" `
        -Method GET
    
    Show-Result "Historique Audit (derniers 50 logs)" $auditHistory $true
    
    # Afficher les détails des logs récents
    Write-Host ""
    Write-Host "--- Détails des logs récents ---" -ForegroundColor Cyan
    if ($auditHistory.auditLogs -and $auditHistory.auditLogs.Count -gt 0) {
        $recentLogs = $auditHistory.auditLogs | Select-Object -First 10
        foreach ($log in $recentLogs) {
            Write-Host ""
            Write-Host "ID: $($log.id)" -ForegroundColor White
            Write-Host "  Service: $($log.serviceName)" -ForegroundColor Gray
            Write-Host "  Action: $($log.actionType)" -ForegroundColor Gray
            Write-Host "  Status: $($log.status)" -ForegroundColor Gray
            Write-Host "  UserId: $($log.userId)" -ForegroundColor Gray
            Write-Host "  CorrelationId: $($log.correlationId)" -ForegroundColor Gray
            Write-Host "  Timestamp: $($log.timestamp)" -ForegroundColor Gray
            if ($log.description) {
                Write-Host "  Description: $($log.description)" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "Aucun log trouvé dans l'historique" -ForegroundColor Yellow
    }
    
} catch {
    Write-Host "ERREUR lors de la récupération de l'historique audit:" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

# ============================================
# 4. VERIFICATION PAR SERVICE
# ============================================
Write-Host ""
Write-Host "4. VERIFICATION PAR SERVICE" -ForegroundColor Yellow

# Vérifier les logs du Payment Service
Write-Host "Logs du Payment Service:" -ForegroundColor White
try {
    $paymentLogs = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/history?serviceName=payment-service&size=10" `
        -Method GET
    
    if ($paymentLogs.auditLogs -and $paymentLogs.auditLogs.Count -gt 0) {
        Write-Host "  ✓ $($paymentLogs.totalElements) log(s) trouvé(s)" -ForegroundColor Green
        foreach ($log in $paymentLogs.auditLogs | Select-Object -First 5) {
            Write-Host "    - $($log.actionType): $($log.status) ($($log.timestamp))" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ✗ Aucun log trouvé pour Payment Service" -ForegroundColor Red
    }
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
}

# Vérifier les logs du Crypto Service
Write-Host "Logs du Crypto Service:" -ForegroundColor White
try {
    $cryptoLogs = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/history?serviceName=crypto-service&size=10" `
        -Method GET
    
    if ($cryptoLogs.auditLogs -and $cryptoLogs.auditLogs.Count -gt 0) {
        Write-Host "  ✓ $($cryptoLogs.totalElements) log(s) trouvé(s)" -ForegroundColor Green
        foreach ($log in $cryptoLogs.auditLogs | Select-Object -First 5) {
            Write-Host "    - $($log.actionType): $($log.status) ($($log.timestamp))" -ForegroundColor Gray
        }
    } else {
        Write-Host "  ✗ Aucun log trouvé pour Crypto Service" -ForegroundColor Red
    }
} catch {
    Write-Host "  ✗ Erreur: $($_.Exception.Message)" -ForegroundColor Red
}

# ============================================
# 5. STATISTIQUES
# ============================================
Write-Host ""
Write-Host "5. STATISTIQUES" -ForegroundColor Yellow

try {
    $stats = Invoke-RestMethod -Uri "$auditServiceUrl/api/audit/stats/errors" `
        -Method GET
    
    Write-Host "Total d'erreurs: $($stats.totalErrors)" -ForegroundColor $(if ($stats.totalErrors -gt 0) { "Red" } else { "Green" })
} catch {
    Write-Host "Impossible de récupérer les statistiques" -ForegroundColor Yellow
}

# ============================================
# RESUME
# ============================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  RESUME DU TEST" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "✓ Payment Service: $paymentServiceUrl" -ForegroundColor $(if ($paymentResponse) { "Green" } else { "Red" })
Write-Host "✓ Crypto Service: $cryptoServiceUrl" -ForegroundColor $(if ($cryptoResponse) { "Green" } else { "Red" })
Write-Host "✓ Audit Service: $auditServiceUrl" -ForegroundColor Green
Write-Host ""
Write-Host "Les opérations ont été testées." -ForegroundColor Yellow
Write-Host "Vérifiez l'historique d'audit ci-dessus pour confirmer l'enregistrement." -ForegroundColor Yellow
Write-Host ""

