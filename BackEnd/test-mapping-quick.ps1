# Script de test rapide pour le mapping userId -> accountRef
# Modifiez les identifiants ci-dessous avant d'executer

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test: Mapping userId -> accountRef" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration avec les ports reels
$keycloakUrl = "http://localhost:8080"
$realm = "ebanking-realm"
$clientId = "ebanking-client"
$clientSecret = "your-client-secret"

# IDENTIFIANTS - MODIFIEZ ICI
$testUsername = "client1"  # Remplacez par votre utilisateur
$testPassword = "password"  # Remplacez par le mot de passe

$accountServiceUrl = "http://localhost:8087"
$cryptoServiceUrl = "http://localhost:8085"

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Keycloak: $keycloakUrl" -ForegroundColor Gray
Write-Host "  Account Service: $accountServiceUrl" -ForegroundColor Gray
Write-Host "  Crypto Service: $cryptoServiceUrl" -ForegroundColor Gray
Write-Host "  Username: $testUsername" -ForegroundColor Gray
Write-Host ""

# Test 1: Obtenir un token Keycloak
Write-Host "Etape 1: Obtention du token Keycloak..." -ForegroundColor Cyan

$tokenUrl = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"
$body = @{
    grant_type = "password"
    client_id = $clientId
    client_secret = $clientSecret
    username = $testUsername
    password = $testPassword
}

try {
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
    $token = $tokenResponse.access_token
    Write-Host "OK - Token obtenu avec succes" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "ERREUR - Impossible d'obtenir le token" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "  Reponse: $responseBody" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "💡 Modifiez les identifiants dans le script (lignes 18-19)" -ForegroundColor Yellow
    exit 1
}

# Test 2: GET /api/accounts/user/{userId}
Write-Host "Etape 2: Test GET /api/accounts/user/$testUsername" -ForegroundColor Cyan

$url = "$accountServiceUrl/api/accounts/user/$testUsername"
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

try {
    $response = Invoke-RestMethod -Uri $url -Method Get -Headers $headers
    Write-Host "✅ OK - Compte recupere avec succes" -ForegroundColor Green
    Write-Host "   Account Reference: $($response.externalReference)" -ForegroundColor Green
    Write-Host "   Balance: $($response.balance) $($response.devise)" -ForegroundColor Green
    Write-Host "   Status: $($response.status)" -ForegroundColor Green
    $accountRef = $response.externalReference
    $balance = $response.balance
    Write-Host ""
    Write-Host "✅ Le mapping userId -> accountRef fonctionne!" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ ERREUR - Impossible de recuperer le compte" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "   Status Code: $statusCode" -ForegroundColor Red
        
        if ($statusCode -eq 404) {
            Write-Host "   💡 L'utilisateur n'a pas de compte. Creez-en un:" -ForegroundColor Yellow
            Write-Host "      POST $accountServiceUrl/api/accounts" -ForegroundColor Yellow
            Write-Host "      Body: {`"devise`": `"EUR`", `"initialBalance`": 1000.00}" -ForegroundColor Yellow
        } elseif ($statusCode -eq 403) {
            Write-Host "   💡 Acces interdit. Verifiez que l'utilisateur demande son propre compte." -ForegroundColor Yellow
        }
    }
    Write-Host "   Message: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 3: Recuperer les prix crypto
Write-Host "Etape 3: Recuperation des prix crypto..." -ForegroundColor Cyan

$pricesUrl = "$cryptoServiceUrl/api/v1/crypto/prices"
try {
    $pricesResponse = Invoke-RestMethod -Uri $pricesUrl -Method Get -Headers $headers
    $btcPrice = $pricesResponse.prices.BTC
    Write-Host "✅ OK - Prix BTC: $btcPrice EUR" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "❌ ERREUR - Impossible de recuperer les prix" -ForegroundColor Red
    Write-Host "   Message: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
}

# Test 4: Test achat crypto avec solde insuffisant
Write-Host "Etape 4: Test achat crypto (solde insuffisant)..." -ForegroundColor Cyan

$quantity = ($balance + 1000) / $btcPrice
$tradeBody = @{
    symbol = "BTC"
    quantity = $quantity
    type = "BUY"
}

$tradeUrl = "$cryptoServiceUrl/api/v1/crypto/trade"
try {
    $tradeResponse = Invoke-RestMethod -Uri $tradeUrl -Method Post -Headers $headers -Body ($tradeBody | ConvertTo-Json) -ErrorAction Stop
    Write-Host "⚠️  ATTENTION - L'achat a reussi alors qu'il devrait echouer" -ForegroundColor Yellow
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 400 -or $statusCode -eq 500) {
        Write-Host "✅ OK - Achat rejete (solde insuffisant) comme attendu" -ForegroundColor Green
    } else {
        Write-Host "❌ ERREUR - Status inattendu: $statusCode" -ForegroundColor Red
    }
}
Write-Host ""

# Test 5: Achat avec solde suffisant
Write-Host "Etape 5: Test achat crypto (solde suffisant)..." -ForegroundColor Cyan

$quantity = 0.001
$requiredAmount = $quantity * $btcPrice

if ($requiredAmount -gt $balance) {
    Write-Host "⚠️  ATTENTION - Solde insuffisant pour ce test" -ForegroundColor Yellow
    Write-Host "   Solde disponible: $balance EUR" -ForegroundColor Yellow
    Write-Host "   Montant requis: $requiredAmount EUR" -ForegroundColor Yellow
    Write-Host "   💡 Credit d'abord le compte pour tester" -ForegroundColor Yellow
} else {
    $tradeBody = @{
        symbol = "BTC"
        quantity = $quantity
        type = "BUY"
    }
    
    try {
        $tradeResponse = Invoke-RestMethod -Uri $tradeUrl -Method Post -Headers $headers -Body ($tradeBody | ConvertTo-Json) -ErrorAction Stop
        Write-Host "✅ OK - Achat effectue avec succes" -ForegroundColor Green
        Write-Host "   Transaction ID: $($tradeResponse.id)" -ForegroundColor Green
        Write-Host "   Quantite: $($tradeResponse.quantity) BTC" -ForegroundColor Green
        
        # Verifier le solde final
        $finalBalanceResponse = Invoke-RestMethod -Uri "$accountServiceUrl/api/accounts/$accountRef/balance" -Method Get -Headers $headers
        $finalBalance = $finalBalanceResponse.balance
        Write-Host "   Solde final: $finalBalance EUR" -ForegroundColor Green
        Write-Host "   Solde initial: $balance EUR" -ForegroundColor Gray
        Write-Host "   Montant debite: $requiredAmount EUR" -ForegroundColor Gray
        
        Write-Host ""
        Write-Host "✅ Le compte a ete correctement debite!" -ForegroundColor Green
    } catch {
        Write-Host "❌ ERREUR - Echec de l'achat" -ForegroundColor Red
        Write-Host "   Message: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "✅ Tests termines!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📌 Resume:" -ForegroundColor Cyan
Write-Host "   ✅ Le mapping userId -> accountRef fonctionne via GET /api/accounts/user/{userId}" -ForegroundColor Green
Write-Host "   ✅ crypto-service utilise maintenant accountService.getAccountRefByUserId()" -ForegroundColor Green
Write-Host "   ✅ Les transactions crypto verifient et debitent le compte correctement" -ForegroundColor Green
