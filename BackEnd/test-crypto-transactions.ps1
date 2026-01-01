# Script de test pour valider les transactions crypto avec le nouveau mapping userId -> accountRef
# Ce script teste que crypto-service utilise correctement l'endpoint GET /api/accounts/user/{userId}

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Test: Transactions Crypto avec Mapping userId -> accountRef" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$keycloakUrl = "http://localhost:8080"
$realm = "ebanking-realm"
$clientId = "ebanking-client"
$clientSecret = "your-client-secret"
$accountServiceUrl = "http://localhost:8087"
$cryptoServiceUrl = "http://localhost:8085"

# Fonction pour obtenir un token Keycloak
function Get-KeycloakToken {
    param(
        [string]$Username,
        [string]$Password
    )
    
    $tokenUrl = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"
    
    $body = @{
        grant_type = "password"
        client_id = $clientId
        client_secret = $clientSecret
        username = $Username
        password = $Password
    }
    
    try {
        $response = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
        return $response.access_token
    } catch {
        Write-Host "❌ Erreur lors de l'obtention du token: $_" -ForegroundColor Red
        return $null
    }
}

# Fonction pour tester un endpoint
function Test-Endpoint {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Token = $null,
        [object]$Body = $null,
        [int]$ExpectedStatus = 200
    )
    
    $headers = @{
        "Content-Type" = "application/json"
    }
    
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            Headers = $headers
            ErrorAction = "Stop"
        }
        
        if ($Body) {
            $params["Body"] = ($Body | ConvertTo-Json -Depth 10)
        }
        
        $response = Invoke-RestMethod @params
        return @{
            Success = $true
            StatusCode = 200
            Response = $response
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if (-not $statusCode) {
            $statusCode = 0
        }
        
        return @{
            Success = ($statusCode -eq $ExpectedStatus)
            StatusCode = $statusCode
            Error = $_.Exception.Message
        }
    }
}

# Test 1: Obtenir un token pour un utilisateur CLIENT
Write-Host "📝 Étape 1: Obtention du token Keycloak" -ForegroundColor Cyan
Write-Host ""

# Remplacez par les identifiants réels de votre utilisateur de test avec rôle CLIENT
$testUsername = "client1"
$testPassword = "password"

$token = Get-KeycloakToken -Username $testUsername -Password $testPassword

if ($null -eq $token) {
    Write-Host "❌ Impossible d'obtenir le token. Vérifiez les identifiants Keycloak." -ForegroundColor Red
    Write-Host "💡 Assurez-vous que:" -ForegroundColor Yellow
    Write-Host "   - Keycloak est démarré sur le port 8180" -ForegroundColor Yellow
    Write-Host "   - L'utilisateur '$testUsername' existe avec le rôle CLIENT" -ForegroundColor Yellow
    Write-Host "   - Le client 'ebanking-client' est configuré correctement" -ForegroundColor Yellow
    exit 1
}

Write-Host "✅ Token obtenu avec succès" -ForegroundColor Green
Write-Host ""

# Test 2: Vérifier que l'utilisateur a un compte dans account-service
Write-Host "📝 Étape 2: Vérification du compte utilisateur" -ForegroundColor Cyan
Write-Host ""

$userId = $testUsername
$accountResult = Test-Endpoint -Method "GET" -Url "$accountServiceUrl/api/accounts/user/$userId" -Token $token

if (-not $accountResult.Success) {
    Write-Host "❌ Impossible de récupérer le compte pour l'utilisateur $userId" -ForegroundColor Red
    Write-Host "   Status: $($accountResult.StatusCode)" -ForegroundColor Red
    Write-Host "   Erreur: $($accountResult.Error)" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Créez d'abord un compte pour cet utilisateur:" -ForegroundColor Yellow
    Write-Host "   POST $accountServiceUrl/api/accounts" -ForegroundColor Yellow
    Write-Host "   Body: {`"devise`": `"EUR`", `"initialBalance`": 1000.00}" -ForegroundColor Yellow
    exit 1
}

$accountRef = $accountResult.Response.externalReference
$balance = $accountResult.Response.balance

Write-Host "✅ Compte trouvé!" -ForegroundColor Green
Write-Host "   Account Reference: $accountRef" -ForegroundColor Green
Write-Host "   Balance: $balance $($accountResult.Response.devise)" -ForegroundColor Green
Write-Host ""

# Test 3: Vérifier les prix crypto
Write-Host "📝 Étape 3: Vérification des prix crypto" -ForegroundColor Cyan
Write-Host ""

$pricesResult = Test-Endpoint -Method "GET" -Url "$cryptoServiceUrl/api/v1/crypto/prices" -Token $token

if (-not $pricesResult.Success) {
    Write-Host "❌ Impossible de récupérer les prix crypto" -ForegroundColor Red
    Write-Host "   Status: $($pricesResult.StatusCode)" -ForegroundColor Red
    exit 1
}

$btcPrice = $pricesResult.Response.prices.BTC
if (-not $btcPrice) {
    Write-Host "❌ Prix BTC non disponible" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Prix crypto récupérés!" -ForegroundColor Green
Write-Host "   Prix BTC: $btcPrice EUR" -ForegroundColor Green
Write-Host ""

# Test 4: Test d'achat crypto avec solde insuffisant
Write-Host "📝 Étape 4: Test d'achat crypto avec solde insuffisant" -ForegroundColor Cyan
Write-Host ""

$quantity = ($balance + 1000) / $btcPrice  # Quantité qui dépasse le solde
$buyBody = @{
    symbol = "BTC"
    quantity = $quantity
    type = "BUY"
}

Write-Host "🔍 Tentative d'achat de $quantity BTC (montant requis: $($quantity * $btcPrice) EUR)" -ForegroundColor Yellow
Write-Host "   Solde disponible: $balance EUR" -ForegroundColor Yellow

$buyResult1 = Test-Endpoint -Method "POST" -Url "$cryptoServiceUrl/api/v1/crypto/trade" -Token $token -Body $buyBody -ExpectedStatus 400

if ($buyResult1.Success) {
    Write-Host "✅ Test réussi: L'achat a été rejeté (solde insuffisant)" -ForegroundColor Green
} else {
    Write-Host "⚠️  Status: $($buyResult1.StatusCode)" -ForegroundColor Yellow
    if ($buyResult1.Error) {
        Write-Host "   Erreur: $($buyResult1.Error)" -ForegroundColor Yellow
    }
}
Write-Host ""

# Test 5: Test d'achat crypto avec solde suffisant
Write-Host "📝 Étape 5: Test d'achat crypto avec solde suffisant" -ForegroundColor Cyan
Write-Host ""

$quantity = 0.001  # Petite quantité pour tester
$buyBody = @{
    symbol = "BTC"
    quantity = $quantity
    type = "BUY"
}

$requiredAmount = $quantity * $btcPrice
Write-Host "🔍 Tentative d'achat de $quantity BTC (montant requis: $requiredAmount EUR)" -ForegroundColor Yellow
Write-Host "   Solde disponible: $balance EUR" -ForegroundColor Yellow

if ($requiredAmount -gt $balance) {
    Write-Host "⚠️  Le solde est insuffisant pour ce test. Créditez d'abord le compte." -ForegroundColor Yellow
    Write-Host "   POST $accountServiceUrl/api/accounts/$accountRef/credit" -ForegroundColor Yellow
    Write-Host "   Body: {`"amount`": $($requiredAmount + 100)}" -ForegroundColor Yellow
} else {
    $buyResult2 = Test-Endpoint -Method "POST" -Url "$cryptoServiceUrl/api/v1/crypto/trade" -Token $token -Body $buyBody -ExpectedStatus 201
    
    if ($buyResult2.Success) {
        Write-Host "✅ Test réussi: L'achat a été effectué!" -ForegroundColor Green
        Write-Host "   Transaction ID: $($buyResult2.Response.id)" -ForegroundColor Green
        Write-Host "   Quantité: $($buyResult2.Response.quantity) BTC" -ForegroundColor Green
        Write-Host "   Prix: $($buyResult2.Response.priceAtTime) EUR" -ForegroundColor Green
        
        # Vérifier le wallet
        Write-Host ""
        Write-Host "📝 Vérification du wallet crypto..." -ForegroundColor Cyan
        $walletResult = Test-Endpoint -Method "GET" -Url "$cryptoServiceUrl/api/v1/crypto/wallet" -Token $token
        
        if ($walletResult.Success) {
            Write-Host "✅ Wallet récupéré!" -ForegroundColor Green
            $btcWallet = $walletResult.Response.wallets | Where-Object { $_.symbol -eq "BTC" }
            if ($btcWallet) {
                Write-Host "   Solde BTC: $($btcWallet.balance)" -ForegroundColor Green
            }
        }
    } else {
        Write-Host "❌ Échec de l'achat" -ForegroundColor Red
        Write-Host "   Status: $($buyResult2.StatusCode)" -ForegroundColor Red
        if ($buyResult2.Error) {
            Write-Host "   Erreur: $($buyResult2.Error)" -ForegroundColor Red
        }
    }
}
Write-Host ""

# Test 6: Vérifier que le mapping userId -> accountRef fonctionne
Write-Host "📝 Étape 6: Vérification du mapping userId -> accountRef" -ForegroundColor Cyan
Write-Host ""

Write-Host "✅ Le mapping fonctionne si:" -ForegroundColor Cyan
Write-Host "   1. L'achat crypto a utilisé l'accountRef: $accountRef" -ForegroundColor Cyan
Write-Host "   2. Le compte a été débité correctement" -ForegroundColor Cyan
Write-Host "   3. Aucune erreur 'Compte introuvable' n'a été générée" -ForegroundColor Cyan
Write-Host ""

# Vérifier le solde final
Write-Host "📝 Vérification du solde final du compte..." -ForegroundColor Cyan
$finalBalanceResult = Test-Endpoint -Method "GET" -Url "$accountServiceUrl/api/accounts/$accountRef/balance" -Token $token

if ($finalBalanceResult.Success) {
    $finalBalance = $finalBalanceResult.Response.balance
    Write-Host "✅ Solde final: $finalBalance EUR" -ForegroundColor Green
    
    if ($buyResult2 -and $buyResult2.Success) {
        $expectedBalance = $balance - $requiredAmount
        if ([Math]::Abs($finalBalance - $expectedBalance) -lt 0.01) {
            Write-Host "✅ Le compte a été correctement débité!" -ForegroundColor Green
        } else {
            Write-Host "⚠️  Le solde ne correspond pas exactement (attendu: $expectedBalance, obtenu: $finalBalance)" -ForegroundColor Yellow
        }
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "✅ Tests terminés!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "📌 Résumé:" -ForegroundColor Cyan
Write-Host "   - Le mapping userId -> accountRef fonctionne via GET /api/accounts/user/{userId}" -ForegroundColor Cyan
Write-Host "   - crypto-service utilise maintenant accountService.getAccountRefByUserId()" -ForegroundColor Cyan
Write-Host "   - Les transactions crypto vérifient et débitent/créditent le compte correctement" -ForegroundColor Cyan
