# Script de test pour vérifier payment-service et crypto-service avec authentification
# Teste le flux complet : vérification solde → achat crypto → vérification débit

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

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "TEST INTEGRATION PAYMENT & CRYPTO SERVICES" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ÉTAPE 1: Obtenir un token JWT
Write-Host "ETAPE 1: Obtention du token JWT" -ForegroundColor Yellow
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
    Write-Host ""
} catch {
    Write-Host "ERREUR: Impossible d'obtenir le token JWT" -ForegroundColor Red
    Write-Host "   Message: $($_.Exception.Message)" -ForegroundColor Red
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
            $params.Body = ($Body | ConvertTo-Json -Depth 10)
        }
        
        $response = Invoke-RestMethod @params
        Write-Host "    ✅ Succès (HTTP 200/201)" -ForegroundColor Green
        return @{ Success = $true; Response = $response }
    } catch {
        $statusCode = $null
        $errorBody = $null
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode.value__
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                $errorBody = $reader.ReadToEnd()
            } catch {
                $errorBody = "Impossible de lire le body d'erreur"
            }
        }
        
        Write-Host "    ❌ ERREUR" -ForegroundColor Red
        if ($statusCode) {
            Write-Host "    Status Code: $statusCode" -ForegroundColor Red
        }
        if ($errorBody) {
            Write-Host "    Réponse: $errorBody" -ForegroundColor Red
        }
        Write-Host "    Message: $($_.Exception.Message)" -ForegroundColor Red
        return @{ Success = $false; StatusCode = $statusCode; Error = $errorBody }
    }
}

# ÉTAPE 2: Récupérer les informations utilisateur
Write-Host "ETAPE 2: Récupération des informations utilisateur" -ForegroundColor Yellow
Write-Host ""

$userInfoResult = Test-Endpoint -ServiceName "GET /api/customers/me" -Url "$userServiceUrl/api/customers/me"
if (-not $userInfoResult.Success) {
    Write-Host "ERREUR: Impossible de récupérer les infos utilisateur" -ForegroundColor Red
    exit 1
}

$userInfo = $userInfoResult.Response
Write-Host "  Username: $($userInfo.username)" -ForegroundColor Green
Write-Host "  Email: $($userInfo.email)" -ForegroundColor Green
Write-Host ""

# ÉTAPE 3: Récupérer le compte bancaire
Write-Host "ETAPE 3: Récupération du compte bancaire" -ForegroundColor Yellow
Write-Host ""

$accountResult = Test-Endpoint -ServiceName "GET /api/accounts/user/$username" -Url "$accountServiceUrl/api/accounts/user/$username"
if (-not $accountResult.Success) {
    Write-Host "⚠️  Aucun compte trouvé, création d'un compte EUR..." -ForegroundColor Yellow
    
    # Créer un compte EUR
    $createAccountBody = @{
        devise = "EUR"
        initialBalance = 10000.00
    }
    
    $createResult = Test-Endpoint -ServiceName "POST /api/accounts (création)" -Url "$accountServiceUrl/api/accounts" -Method "POST" -Body $createAccountBody
    if (-not $createResult.Success) {
        Write-Host "ERREUR: Impossible de créer un compte" -ForegroundColor Red
        exit 1
    }
    
    $account = $createResult.Response
    Write-Host "  ✅ Compte créé" -ForegroundColor Green
} else {
    $account = $accountResult.Response
    Write-Host "  ✅ Compte trouvé" -ForegroundColor Green
}

$accountRef = $account.externalReference
$initialBalance = [decimal]$account.balance

Write-Host "  Account Reference: $accountRef" -ForegroundColor Green
Write-Host "  Solde initial: $initialBalance EUR" -ForegroundColor Green
Write-Host ""

# ÉTAPE 4: Vérifier le solde via l'endpoint balance
Write-Host "ETAPE 4: Vérification du solde via /balance" -ForegroundColor Yellow
Write-Host ""

$balanceResult = Test-Endpoint -ServiceName "GET /api/accounts/$accountRef/balance" -Url "$accountServiceUrl/api/accounts/$accountRef/balance"
if (-not $balanceResult.Success) {
    Write-Host "ERREUR: Impossible de récupérer le solde" -ForegroundColor Red
    exit 1
}

$balanceInfo = $balanceResult.Response
$currentBalance = [decimal]$balanceInfo.balance
Write-Host "  Solde actuel: $currentBalance $($balanceInfo.devise)" -ForegroundColor Green
Write-Host ""

# ÉTAPE 5: Créditer le compte si nécessaire (pour avoir de l'argent pour les tests)
if ($currentBalance -lt 1000) {
    Write-Host "ETAPE 5: Crédit du compte (solde insuffisant pour les tests)" -ForegroundColor Yellow
    Write-Host ""
    
    $creditBody = @{
        amount = 5000.00
    }
    
    $creditResult = Test-Endpoint -ServiceName "POST /api/accounts/$accountRef/credit" -Url "$accountServiceUrl/api/accounts/$accountRef/credit" -Method "POST" -Body $creditBody
    if ($creditResult.Success) {
        Write-Host "  ✅ Compte crédité de 5000 EUR" -ForegroundColor Green
        
        # Vérifier le nouveau solde
        $balanceResult = Test-Endpoint -ServiceName "GET /api/accounts/$accountRef/balance" -Url "$accountServiceUrl/api/accounts/$accountRef/balance"
        if ($balanceResult.Success) {
            $currentBalance = [decimal]$balanceResult.Response.balance
            Write-Host "  Nouveau solde: $currentBalance EUR" -ForegroundColor Green
        }
    }
    Write-Host ""
}

# ÉTAPE 6: Récupérer les prix crypto
Write-Host "ETAPE 6: Récupération des prix crypto" -ForegroundColor Yellow
Write-Host ""

$pricesResult = Test-Endpoint -ServiceName "GET /api/v1/crypto/prices" -Url "$cryptoServiceUrl/api/v1/crypto/prices"
if (-not $pricesResult.Success) {
    Write-Host "ERREUR: Impossible de récupérer les prix crypto" -ForegroundColor Red
    exit 1
}

$prices = $pricesResult.Response.prices
$btcPrice = [decimal]$prices.BTC
Write-Host "  Prix BTC: $btcPrice EUR" -ForegroundColor Green
Write-Host ""

# ÉTAPE 7: Test achat crypto (BUY)
Write-Host "ETAPE 7: Test achat crypto (BUY)" -ForegroundColor Yellow
Write-Host ""

# Calculer la quantité à acheter (pour un montant de 100 EUR)
$amountToSpend = 100.00
$quantityToBuy = [math]::Round($amountToSpend / $btcPrice, 8)

Write-Host "  Montant à dépenser: $amountToSpend EUR" -ForegroundColor Cyan
Write-Host "  Quantité BTC à acheter: $quantityToBuy BTC" -ForegroundColor Cyan
Write-Host ""

# Vérifier le solde avant l'achat
$balanceBefore = Test-Endpoint -ServiceName "GET /api/accounts/$accountRef/balance (avant achat)" -Url "$accountServiceUrl/api/accounts/$accountRef/balance"
$balanceBeforeAmount = [decimal]$balanceBefore.Response.balance
Write-Host "  Solde avant achat: $balanceBeforeAmount EUR" -ForegroundColor Gray
Write-Host ""

# Effectuer l'achat
$tradeRequest = @{
    symbol = "BTC"
    type = "BUY"
    quantity = $quantityToBuy
}

$tradeResult = Test-Endpoint -ServiceName "POST /api/v1/crypto/trade (BUY)" -Url "$cryptoServiceUrl/api/v1/crypto/trade" -Method "POST" -Body $tradeRequest

if ($tradeResult.Success) {
    $transaction = $tradeResult.Response
    Write-Host "  ✅ Achat crypto réussi!" -ForegroundColor Green
    Write-Host "    Transaction ID: $($transaction.id)" -ForegroundColor Green
    Write-Host "    Quantité: $($transaction.quantity) BTC" -ForegroundColor Green
    Write-Host "    Prix: $($transaction.priceAtTime) EUR" -ForegroundColor Green
    Write-Host "    Type: $($transaction.type)" -ForegroundColor Green
    Write-Host ""
    
    # ÉTAPE 8: Vérifier que le solde a été débité
    Write-Host "ETAPE 8: Vérification du débit du compte" -ForegroundColor Yellow
    Write-Host ""
    
    Start-Sleep -Seconds 2  # Attendre que la transaction soit traitée
    
    $balanceAfter = Test-Endpoint -ServiceName "GET /api/accounts/$accountRef/balance (après achat)" -Url "$accountServiceUrl/api/accounts/$accountRef/balance"
    if ($balanceAfter.Success) {
        $balanceAfterAmount = [decimal]$balanceAfter.Response.balance
        $debitAmount = $balanceBeforeAmount - $balanceAfterAmount
        
        Write-Host "  Solde après achat: $balanceAfterAmount EUR" -ForegroundColor Green
        Write-Host "  Montant débité: $debitAmount EUR" -ForegroundColor Green
        
        if ($debitAmount -gt 0) {
            Write-Host "  ✅ Le solde a bien été débité!" -ForegroundColor Green
        } else {
            Write-Host "  ⚠️  Le solde n'a pas été débité" -ForegroundColor Yellow
        }
    }
    Write-Host ""
} else {
    Write-Host "  ❌ Échec de l'achat crypto" -ForegroundColor Red
    Write-Host ""
}

# ÉTAPE 9: Vérifier le wallet crypto
Write-Host "ETAPE 9: Vérification du wallet crypto" -ForegroundColor Yellow
Write-Host ""

$walletResult = Test-Endpoint -ServiceName "GET /api/v1/crypto/wallet" -Url "$cryptoServiceUrl/api/v1/crypto/wallet"
if ($walletResult.Success) {
    $wallet = $walletResult.Response
    Write-Host "  ✅ Wallet récupéré" -ForegroundColor Green
    Write-Host "    User ID: $($wallet.userId)" -ForegroundColor Green
    
    if ($wallet.wallets -and $wallet.wallets.Count -gt 0) {
        Write-Host "    Cryptos possédées:" -ForegroundColor Green
        foreach ($item in $wallet.wallets) {
            Write-Host "      - $($item.symbol): $($item.balance)" -ForegroundColor Green
        }
    } else {
        Write-Host "    Aucune crypto possédée" -ForegroundColor Yellow
    }
}
Write-Host ""

# ÉTAPE 10: Test création de paiement (payment-service)
Write-Host "ETAPE 10: Test création de paiement" -ForegroundColor Yellow
Write-Host ""

# Pour le paiement, on a besoin d'un IBAN de destination
# On va utiliser un IBAN fictif pour le test
$destinationIban = "FR1420041010050500013M02606"

$paymentRequest = @{
    sourceAccountId = $accountRef
    destinationIban = $destinationIban
    amount = 50.00
    type = "STANDARD"
}

Write-Host "  Source Account: $accountRef" -ForegroundColor Cyan
Write-Host "  Destination IBAN: $destinationIban" -ForegroundColor Cyan
Write-Host "  Montant: 50.00 EUR" -ForegroundColor Cyan
Write-Host ""

$paymentResult = Test-Endpoint -ServiceName "POST /api/v1/payments" -Url "$paymentServiceUrl/api/v1/payments" -Method "POST" -Body $paymentRequest

if ($paymentResult.Success) {
    $payment = $paymentResult.Response
    Write-Host "  ✅ Paiement créé avec succès!" -ForegroundColor Green
    Write-Host "    Payment ID: $($payment.id)" -ForegroundColor Green
    Write-Host "    Status: $($payment.status)" -ForegroundColor Green
    Write-Host "    Message: $($payment.message)" -ForegroundColor Green
    Write-Host ""
    
    if ($payment.status -eq "REJECTED") {
        Write-Host "  ⚠️  Le paiement a été rejeté (règle anti-fraude)" -ForegroundColor Yellow
        Write-Host "    Raison possible: Montant > 10000 EUR ou autre règle" -ForegroundColor Yellow
    }
} else {
    Write-Host "  ❌ Échec de la création du paiement" -ForegroundColor Red
    Write-Host ""
}

# ÉTAPE 11: Vérifier le solde final
Write-Host "ETAPE 11: Vérification du solde final" -ForegroundColor Yellow
Write-Host ""

$balanceFinal = Test-Endpoint -ServiceName "GET /api/accounts/$accountRef/balance (final)" -Url "$accountServiceUrl/api/accounts/$accountRef/balance"
if ($balanceFinal.Success) {
    $finalBalance = [decimal]$balanceFinal.Response.balance
    Write-Host "  Solde final: $finalBalance EUR" -ForegroundColor Green
    Write-Host ""
}

# RÉSUMÉ FINAL
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "RÉSUMÉ DES TESTS" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$tests = @(
    @{ Name = "Authentification JWT"; Status = $true }
    @{ Name = "Récupération infos utilisateur"; Status = $userInfoResult.Success }
    @{ Name = "Récupération compte bancaire"; Status = $accountResult.Success -or $createResult.Success }
    @{ Name = "Vérification solde"; Status = $balanceResult.Success }
    @{ Name = "Récupération prix crypto"; Status = $pricesResult.Success }
    @{ Name = "Achat crypto (BUY)"; Status = $tradeResult.Success }
    @{ Name = "Vérification débit compte"; Status = $balanceAfter.Success }
    @{ Name = "Récupération wallet crypto"; Status = $walletResult.Success }
    @{ Name = "Création paiement"; Status = $paymentResult.Success }
)

$successCount = ($tests | Where-Object { $_.Status -eq $true }).Count
$totalTests = $tests.Count

Write-Host "Tests effectués:" -ForegroundColor White
foreach ($test in $tests) {
    $status = if ($test.Status) { "✅" } else { "❌" }
    Write-Host "  $status $($test.Name)" -ForegroundColor $(if ($test.Status) { "Green" } else { "Red" })
}
Write-Host ""
Write-Host "Total: $successCount/$totalTests tests réussis" -ForegroundColor $(if ($successCount -eq $totalTests) { "Green" } else { "Yellow" })
Write-Host ""

if ($successCount -eq $totalTests) {
    Write-Host "✅ TOUS LES TESTS RÉUSSIS - L'intégration fonctionne correctement!" -ForegroundColor Green
} else {
    Write-Host "⚠️  Certains tests ont échoué - Vérifiez les erreurs ci-dessus" -ForegroundColor Yellow
}
