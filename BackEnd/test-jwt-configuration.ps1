# Script de test pour vérifier la configuration JWT dans tous les microservices
# Teste que l'authentification JWT fonctionne correctement

$ErrorActionPreference = "Stop"

# Configuration
$keycloakUrl = "http://localhost:8080"
$realm = "ebanking-realm"
$clientId = "ebanking-client"
$username = "user1"
$password = "password"

# URLs des services
$userServiceUrl = "http://localhost:8082"
$accountServiceUrl = "http://localhost:8087"
$paymentServiceUrl = "http://localhost:8086"
$cryptoServiceUrl = "http://localhost:8085"
$auditServiceUrl = "http://localhost:8084"
$authServiceUrl = "http://localhost:8081"
$apiGatewayUrl = "http://localhost:8088"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST CONFIGURATION JWT - TOUS LES SERVICES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ÉTAPE 1: Obtenir un token JWT depuis Keycloak
Write-Host "ETAPE 1: Obtention du token JWT depuis Keycloak" -ForegroundColor Yellow
Write-Host ""

$tokenUrl = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"
$tokenBody = @{
    grant_type = "password"
    client_id = $clientId
    username = $username
    password = $password
}

try {
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $tokenBody -ContentType "application/x-www-form-urlencoded" -ErrorAction Stop
    $accessToken = $tokenResponse.access_token
    
    if ([string]::IsNullOrEmpty($accessToken)) {
        Write-Host "ERREUR: Token non reçu" -ForegroundColor Red
        exit 1
    }
    
    Write-Host "✅ Token JWT obtenu avec succès" -ForegroundColor Green
    Write-Host "   Token (premiers 50 caractères): $($accessToken.Substring(0, [Math]::Min(50, $accessToken.Length)))..." -ForegroundColor Gray
    Write-Host ""
} catch {
    Write-Host "ERREUR: Impossible d'obtenir le token JWT" -ForegroundColor Red
    Write-Host "   Message: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Vérifiez que Keycloak est démarré sur le port 8080" -ForegroundColor Yellow
    exit 1
}

# Headers avec token
$headers = @{
    "Authorization" = "Bearer $accessToken"
    "Content-Type" = "application/json"
}

# Fonction pour tester un endpoint
function Test-Endpoint {
    param(
        [string]$ServiceName,
        [string]$Url,
        [string]$Method = "GET",
        [object]$Body = $null
    )
    
    Write-Host "  Test: $ServiceName" -ForegroundColor Cyan
    Write-Host "    URL: $Url" -ForegroundColor Gray
    Write-Host "    Method: $Method" -ForegroundColor Gray
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $headers
            ErrorAction = "Stop"
        }
        
        if ($Body -ne $null) {
            $params.Body = ($Body | ConvertTo-Json)
        }
        
        $response = Invoke-RestMethod @params
        Write-Host "    ✅ Succès (HTTP 200)" -ForegroundColor Green
        return $true
    } catch {
        $statusCode = $null
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode.value__
        }
        
        if ($statusCode -eq 401) {
            Write-Host "    ❌ ÉCHEC: Non autorisé (HTTP 401) - Token invalide ou expiré" -ForegroundColor Red
        } elseif ($statusCode -eq 403) {
            Write-Host "    ⚠️  Accès refusé (HTTP 403) - Rôle insuffisant" -ForegroundColor Yellow
        } elseif ($statusCode -eq 404) {
            Write-Host "    ⚠️  Endpoint non trouvé (HTTP 404)" -ForegroundColor Yellow
        } elseif ($statusCode -eq 503) {
            Write-Host "    ⚠️  Service indisponible (HTTP 503)" -ForegroundColor Yellow
        } else {
            Write-Host "    ❌ ERREUR: $($_.Exception.Message)" -ForegroundColor Red
            if ($statusCode) {
                Write-Host "    Status Code: $statusCode" -ForegroundColor Red
            }
        }
        return $false
    }
}

# ÉTAPE 2: Tester user-service
Write-Host "ETAPE 2: Test user-service" -ForegroundColor Yellow
Write-Host ""

$userServiceTests = @(
    @{ Name = "GET /api/customers/me"; Url = "$userServiceUrl/api/customers/me" }
)

$userServiceSuccess = 0
foreach ($test in $userServiceTests) {
    if (Test-Endpoint -ServiceName $test.Name -Url $test.Url) {
        $userServiceSuccess++
    }
    Write-Host ""
}

Write-Host "Résultat user-service: $userServiceSuccess/$($userServiceTests.Count) tests réussis" -ForegroundColor $(if ($userServiceSuccess -eq $userServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host ""

# ÉTAPE 3: Tester account-service
Write-Host "ETAPE 3: Test account-service" -ForegroundColor Yellow
Write-Host ""

$accountServiceTests = @(
    @{ Name = "GET /api/accounts/user/user1"; Url = "$accountServiceUrl/api/accounts/user/user1" }
)

$accountServiceSuccess = 0
foreach ($test in $accountServiceTests) {
    if (Test-Endpoint -ServiceName $test.Name -Url $test.Url) {
        $accountServiceSuccess++
    }
    Write-Host ""
}

Write-Host "Résultat account-service: $accountServiceSuccess/$($accountServiceTests.Count) tests réussis" -ForegroundColor $(if ($accountServiceSuccess -eq $accountServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host ""

# ÉTAPE 4: Tester crypto-service
Write-Host "ETAPE 4: Test crypto-service" -ForegroundColor Yellow
Write-Host ""

$cryptoServiceTests = @(
    @{ Name = "GET /api/v1/crypto/prices"; Url = "$cryptoServiceUrl/api/v1/crypto/prices" },
    @{ Name = "GET /api/v1/crypto/wallet"; Url = "$cryptoServiceUrl/api/v1/crypto/wallet" }
)

$cryptoServiceSuccess = 0
foreach ($test in $cryptoServiceTests) {
    if (Test-Endpoint -ServiceName $test.Name -Url $test.Url) {
        $cryptoServiceSuccess++
    }
    Write-Host ""
}

Write-Host "Résultat crypto-service: $cryptoServiceSuccess/$($cryptoServiceTests.Count) tests réussis" -ForegroundColor $(if ($cryptoServiceSuccess -eq $cryptoServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host ""

# ÉTAPE 5: Tester payment-service
Write-Host "ETAPE 5: Test payment-service" -ForegroundColor Yellow
Write-Host ""

# Payment service n'a que POST /api/v1/payments, on teste juste que le service répond
$paymentServiceTests = @(
    @{ Name = "Service accessible (test via Swagger)"; Url = "$paymentServiceUrl/swagger-ui.html" }
)

$paymentServiceSuccess = 0
foreach ($test in $paymentServiceTests) {
    if (Test-Endpoint -ServiceName $test.Name -Url $test.Url) {
        $paymentServiceSuccess++
    }
    Write-Host ""
}

Write-Host "Résultat payment-service: $paymentServiceSuccess/$($paymentServiceTests.Count) tests réussis" -ForegroundColor $(if ($paymentServiceSuccess -eq $paymentServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host ""

# ÉTAPE 6: Tester audit-service
Write-Host "ETAPE 6: Test audit-service" -ForegroundColor Yellow
Write-Host ""

$auditServiceTests = @(
    @{ Name = "GET /api/audit/health"; Url = "$auditServiceUrl/api/audit/health" }
)

$auditServiceSuccess = 0
foreach ($test in $auditServiceTests) {
    if (Test-Endpoint -ServiceName $test.Name -Url $test.Url) {
        $auditServiceSuccess++
    }
    Write-Host ""
}

Write-Host "Résultat audit-service: $auditServiceSuccess/$($auditServiceTests.Count) tests réussis" -ForegroundColor $(if ($auditServiceSuccess -eq $auditServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host ""

# ÉTAPE 7: Tester auth-service
Write-Host "ETAPE 7: Test auth-service" -ForegroundColor Yellow
Write-Host ""

$authServiceTests = @(
    @{ Name = "GET /auth/protected"; Url = "$authServiceUrl/auth/protected" }
)

$authServiceSuccess = 0
foreach ($test in $authServiceTests) {
    if (Test-Endpoint -ServiceName $test.Name -Url $test.Url) {
        $authServiceSuccess++
    }
    Write-Host ""
}

Write-Host "Résultat auth-service: $authServiceSuccess/$($authServiceTests.Count) tests réussis" -ForegroundColor $(if ($authServiceSuccess -eq $authServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host ""

# ÉTAPE 8: Tester API Gateway
Write-Host "ETAPE 8: Test API Gateway" -ForegroundColor Yellow
Write-Host ""

# Test via API Gateway - peut nécessiter une configuration supplémentaire pour la propagation du token
$apiGatewayTests = @(
    @{ Name = "GET /api/customers/me (via Gateway)"; Url = "$apiGatewayUrl/api/customers/me" }
)

$apiGatewaySuccess = 0
foreach ($test in $apiGatewayTests) {
    if (Test-Endpoint -ServiceName $test.Name -Url $test.Url) {
        $apiGatewaySuccess++
    }
    Write-Host ""
}

Write-Host "Résultat API Gateway: $apiGatewaySuccess/$($apiGatewayTests.Count) tests réussis" -ForegroundColor $(if ($apiGatewaySuccess -eq $apiGatewayTests.Count) { "Green" } else { "Yellow" })
Write-Host ""

# ÉTAPE 9: Test sans token (doit échouer)
Write-Host "ETAPE 9: Test sans token (doit échouer avec 401)" -ForegroundColor Yellow
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri "$userServiceUrl/api/customers/me" -Method Get -ErrorAction Stop
    Write-Host "  ❌ ERREUR: L'endpoint devrait être protégé mais a répondu avec succès" -ForegroundColor Red
    Write-Host "     La configuration JWT ne fonctionne pas correctement!" -ForegroundColor Red
} catch {
    $statusCode = $null
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
    }
    
    if ($statusCode -eq 401) {
        Write-Host "  ✅ Correct: L'endpoint est bien protégé (HTTP 401 sans token)" -ForegroundColor Green
    } else {
        Write-Host "  ⚠️  Status Code: $statusCode (attendu: 401)" -ForegroundColor Yellow
    }
}
Write-Host ""

# RÉSUMÉ FINAL
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RÉSUMÉ DES TESTS JWT" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$totalTests = $userServiceTests.Count + $accountServiceTests.Count + $cryptoServiceTests.Count + $paymentServiceTests.Count + $auditServiceTests.Count + $authServiceTests.Count + $apiGatewayTests.Count
$totalSuccess = $userServiceSuccess + $accountServiceSuccess + $cryptoServiceSuccess + $paymentServiceSuccess + $auditServiceSuccess + $authServiceSuccess + $apiGatewaySuccess

Write-Host "Services testés:" -ForegroundColor White
Write-Host "  - user-service:     $userServiceSuccess/$($userServiceTests.Count)" -ForegroundColor $(if ($userServiceSuccess -eq $userServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host "  - account-service:  $accountServiceSuccess/$($accountServiceTests.Count)" -ForegroundColor $(if ($accountServiceSuccess -eq $accountServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host "  - crypto-service:   $cryptoServiceSuccess/$($cryptoServiceTests.Count)" -ForegroundColor $(if ($cryptoServiceSuccess -eq $cryptoServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host "  - payment-service:  $paymentServiceSuccess/$($paymentServiceTests.Count)" -ForegroundColor $(if ($paymentServiceSuccess -eq $paymentServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host "  - audit-service:    $auditServiceSuccess/$($auditServiceTests.Count)" -ForegroundColor $(if ($auditServiceSuccess -eq $auditServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host "  - auth-service:    $authServiceSuccess/$($authServiceTests.Count)" -ForegroundColor $(if ($authServiceSuccess -eq $authServiceTests.Count) { "Green" } else { "Yellow" })
Write-Host "  - api-gateway:     $apiGatewaySuccess/$($apiGatewayTests.Count)" -ForegroundColor $(if ($apiGatewaySuccess -eq $apiGatewayTests.Count) { "Green" } else { "Yellow" })
Write-Host ""
Write-Host "Total: $totalSuccess/$totalTests tests réussis" -ForegroundColor $(if ($totalSuccess -eq $totalTests) { "Green" } else { "Yellow" })
Write-Host ""

if ($totalSuccess -eq $totalTests) {
    Write-Host "✅ CONFIGURATION JWT VALIDÉE - Tous les services fonctionnent correctement!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Certains services ont des problèmes - Vérifiez les erreurs ci-dessus" -ForegroundColor Yellow
}
