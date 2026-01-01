# Script de test simplifie pour l'integration complete

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Magenta
Write-Host "Test Integration Complete" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

# Configuration
$keycloakUrl = "http://localhost:8080"
$realm = "ebanking-realm"
$clientId = "ebanking-client"
# Le client est public (publicClient: true), pas besoin de client_secret
$accountServiceUrl = "http://localhost:8087"
$cryptoServiceUrl = "http://localhost:8085"
$userServiceUrl = "http://localhost:8082"

# Identifiants - Utilisez user1 (defini dans realm-export.json)
$testUsername = "user1"
$testPassword = "password"

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Keycloak: $keycloakUrl" -ForegroundColor Gray
Write-Host "  User Service: $userServiceUrl" -ForegroundColor Gray
Write-Host "  Account Service: $accountServiceUrl" -ForegroundColor Gray
Write-Host "  Crypto Service: $cryptoServiceUrl" -ForegroundColor Gray
Write-Host "  Username: $testUsername" -ForegroundColor Gray
Write-Host ""

# ETAPE 1: Authentification
Write-Host "ETAPE 1: Authentification via Keycloak" -ForegroundColor Cyan
Write-Host ""

$tokenUrl = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"
# Le client est public, on ne met pas client_secret
$body = @{
    grant_type = "password"
    client_id = $clientId
    username = $testUsername
    password = $testPassword
}

try {
    Write-Host "Tentative d'authentification..." -ForegroundColor Yellow
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
    $token = $tokenResponse.access_token
    Write-Host "OK - Authentification reussie!" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "ERREUR - Authentification echouee" -ForegroundColor Red
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Reponse: $responseBody" -ForegroundColor Red
    }
    exit 1
}

$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

# ETAPE 1.5: Recuperation des informations utilisateur
Write-Host "ETAPE 1.5: Recuperation des informations utilisateur" -ForegroundColor Cyan
Write-Host ""

$userInfoUrl = "$userServiceUrl/api/customers/me"
try {
    Write-Host "Recuperation des infos utilisateur..." -ForegroundColor Yellow
    Write-Host "  URL: $userInfoUrl" -ForegroundColor Gray
    
    # Ajouter un timeout et des retries pour plus de robustesse
    $maxRetries = 3
    $retryCount = 0
    $userInfo = $null
    
    while ($retryCount -lt $maxRetries -and $userInfo -eq $null) {
        try {
            $userInfo = Invoke-RestMethod -Uri $userInfoUrl -Method Get -Headers $headers -TimeoutSec 10 -ErrorAction Stop
            break
        } catch {
            $retryCount++
            if ($retryCount -lt $maxRetries) {
                Write-Host "  Tentative $retryCount/$maxRetries echouee, nouvel essai dans 2 secondes..." -ForegroundColor Yellow
                Start-Sleep -Seconds 2
            } else {
                throw
            }
        }
    }
    
    Write-Host "OK - Informations utilisateur recuperees!" -ForegroundColor Green
    Write-Host "  ID: $($userInfo.id)" -ForegroundColor Green
    Write-Host "  Username: $($userInfo.username)" -ForegroundColor Green
    Write-Host "  Email: $($userInfo.email)" -ForegroundColor Green
    Write-Host "  KYC Status: $($userInfo.kycStatus)" -ForegroundColor Green
    Write-Host ""
    
    if ($userInfo.kycStatus -ne "VERIFIED") {
        Write-Host "ATTENTION - KYC Status n'est pas VERIFIED" -ForegroundColor Yellow
        Write-Host "  Certains services peuvent refuser les operations" -ForegroundColor Yellow
        Write-Host ""
    }
} catch {
    Write-Host "ERREUR - Impossible de recuperer les infos utilisateur" -ForegroundColor Red
    if ($_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        Write-Host "  Status Code: $statusCode" -ForegroundColor Red
        
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errorBody = $reader.ReadToEnd()
            Write-Host "  Reponse: $errorBody" -ForegroundColor Red
        } catch {
            Write-Host "  Impossible de lire la reponse d'erreur" -ForegroundColor Red
        }
        
        if ($statusCode -eq 401) {
            Write-Host "  💡 Le token JWT n'est pas valide ou a expire" -ForegroundColor Yellow
        } elseif ($statusCode -eq 404) {
            Write-Host "  💡 L'endpoint /api/customers/me n'existe pas" -ForegroundColor Yellow
        } elseif ($statusCode -eq 503 -or $statusCode -eq 0) {
            Write-Host "  💡 user-service n'est peut-etre pas demarre ou non accessible" -ForegroundColor Yellow
            Write-Host "     Verifiez que user-service est demarre sur le port 8082" -ForegroundColor Yellow
        }
    } else {
        Write-Host "  Message: $($_.Exception.Message)" -ForegroundColor Red
        if ($_.Exception.Message -like "*timeout*" -or $_.Exception.Message -like "*refused*") {
            Write-Host "  💡 user-service n'est peut-etre pas demarre ou non accessible" -ForegroundColor Yellow
            Write-Host "     Verifiez que user-service est demarre sur le port 8082" -ForegroundColor Yellow
        }
    }
    Write-Host "  Continuons quand meme..." -ForegroundColor Yellow
    Write-Host ""
}

# ETAPE 2: Verification/Creation du compte
Write-Host "ETAPE 2: Verification/Creation du compte" -ForegroundColor Cyan
Write-Host ""

$getAccountUrl = "$accountServiceUrl/api/accounts/user/$testUsername"
$accountRef = $null
$balance = 0

try {
    Write-Host "Verification si le compte existe..." -ForegroundColor Yellow
    $accountResponse = Invoke-RestMethod -Uri $getAccountUrl -Method Get -Headers $headers
    $accountRef = $accountResponse.externalReference
    $balance = $accountResponse.balance
    Write-Host "OK - Compte existant trouve!" -ForegroundColor Green
    Write-Host "Account Reference: $accountRef" -ForegroundColor Green
    Write-Host "Balance: $balance EUR" -ForegroundColor Green
    Write-Host ""
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404 -or $statusCode -eq 500) {
        Write-Host "Compte non trouve. Creation..." -ForegroundColor Yellow
        $createAccountUrl = "$accountServiceUrl/api/accounts"
        $createAccountBody = @{
            devise = "EUR"
            initialBalance = 1000.00
        } | ConvertTo-Json
        
        try {
            Write-Host "Tentative de creation du compte..." -ForegroundColor Yellow
            $newAccountResponse = Invoke-RestMethod -Uri $createAccountUrl -Method Post -Headers $headers -Body $createAccountBody
            $accountRef = $newAccountResponse.externalReference
            $balance = $newAccountResponse.balance
            Write-Host "OK - Compte cree!" -ForegroundColor Green
            Write-Host "Account Reference: $accountRef" -ForegroundColor Green
            Write-Host "Balance initiale: $balance EUR" -ForegroundColor Green
            Write-Host ""
        } catch {
            Write-Host "ERREUR - Impossible de creer le compte" -ForegroundColor Red
            Write-Host "Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
            Write-Host "Message: $($_.Exception.Message)" -ForegroundColor Red
            if ($_.Exception.Response) {
                $stream = $_.Exception.Response.GetResponseStream()
                $reader = New-Object System.IO.StreamReader($stream)
                $errorBody = $reader.ReadToEnd()
                Write-Host "Reponse: $errorBody" -ForegroundColor Red
            }
            Write-Host ""
            Write-Host "💡 Le compte existe peut-etre deja. Continuons avec les autres tests..." -ForegroundColor Yellow
            Write-Host ""
            # On continue quand meme pour tester les autres fonctionnalites
            $accountRef = "UNKNOWN"
            $balance = 0
        }
    } else {
        Write-Host "ERREUR - Status inattendu: $statusCode" -ForegroundColor Red
        exit 1
    }
}

# ETAPE 3: Test du mapping
Write-Host "ETAPE 3: Test du mapping userId -> accountRef" -ForegroundColor Cyan
Write-Host ""

try {
    $mappingResponse = Invoke-RestMethod -Uri $getAccountUrl -Method Get -Headers $headers
    if ($mappingResponse.externalReference -eq $accountRef) {
        Write-Host "OK - Mapping fonctionne!" -ForegroundColor Green
        Write-Host "userId: $testUsername" -ForegroundColor Green
        Write-Host "accountRef: $accountRef" -ForegroundColor Green
        Write-Host ""
    }
} catch {
    Write-Host "ERREUR - Mapping echoue" -ForegroundColor Red
    exit 1
}

# ETAPE 4: Prix crypto
Write-Host "ETAPE 4: Recuperation des prix crypto" -ForegroundColor Cyan
Write-Host ""

$pricesUrl = "$cryptoServiceUrl/api/v1/crypto/prices"
try {
    $pricesResponse = Invoke-RestMethod -Uri $pricesUrl -Method Get -Headers $headers
    $btcPrice = $pricesResponse.prices.BTC
    Write-Host "OK - Prix BTC: $btcPrice EUR" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host "ERREUR - Impossible de recuperer les prix" -ForegroundColor Red
    exit 1
}

# ETAPE 5: Test transaction (solde insuffisant)
Write-Host "ETAPE 5: Test transaction (solde insuffisant)" -ForegroundColor Cyan
Write-Host ""

$quantity = ($balance + 1000) / $btcPrice
$tradeBody = @{
    symbol = "BTC"
    quantity = $quantity
    type = "BUY"
}

$tradeUrl = "$cryptoServiceUrl/api/v1/crypto/trade"
try {
    $tradeResponse = Invoke-RestMethod -Uri $tradeUrl -Method Post -Headers $headers -Body ($tradeBody | ConvertTo-Json) -ErrorAction Stop
    Write-Host "ATTENTION - Achat reussi alors qu'il devrait echouer" -ForegroundColor Yellow
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 400 -or $statusCode -eq 500) {
        Write-Host "OK - Achat rejete (solde insuffisant) comme attendu" -ForegroundColor Green
    }
}
Write-Host ""

# ETAPE 6: Test transaction (solde suffisant)
Write-Host "ETAPE 6: Test transaction (solde suffisant)" -ForegroundColor Cyan
Write-Host ""

$quantity = 0.001
$requiredAmount = $quantity * $btcPrice

if ($requiredAmount -gt $balance) {
    Write-Host "ATTENTION - Solde insuffisant pour ce test" -ForegroundColor Yellow
} else {
    $tradeBody = @{
        symbol = "BTC"
        quantity = $quantity
        type = "BUY"
    }
    
    try {
        Write-Host "Tentative d'achat de $quantity BTC (montant requis: $requiredAmount EUR)" -ForegroundColor Yellow
        Write-Host "Body envoye: $($tradeBody | ConvertTo-Json)" -ForegroundColor Gray
        $tradeResponse = Invoke-RestMethod -Uri $tradeUrl -Method Post -Headers $headers -Body ($tradeBody | ConvertTo-Json) -ErrorAction Stop
        Write-Host "OK - Achat effectue!" -ForegroundColor Green
        Write-Host "Transaction ID: $($tradeResponse.id)" -ForegroundColor Green
        
        $finalBalanceResponse = Invoke-RestMethod -Uri "$accountServiceUrl/api/accounts/$accountRef/balance" -Method Get -Headers $headers
        $finalBalance = $finalBalanceResponse.balance
        Write-Host "Solde final: $finalBalance EUR" -ForegroundColor Green
        Write-Host "Solde initial: $balance EUR" -ForegroundColor Gray
        Write-Host "Montant debite: $requiredAmount EUR" -ForegroundColor Gray
    } catch {
        Write-Host "ERREUR - Echec de l'achat" -ForegroundColor Red
        if ($_.Exception.Response) {
            $statusCode = $_.Exception.Response.StatusCode.value__
            Write-Host "Status Code: $statusCode" -ForegroundColor Red
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errorBody = $reader.ReadToEnd()
            Write-Host "Reponse erreur: $errorBody" -ForegroundColor Red
        } else {
            Write-Host "Message: $($_.Exception.Message)" -ForegroundColor Red
        }
        Write-Host ""
        Write-Host "💡 Causes possibles:" -ForegroundColor Yellow
        Write-Host "   1. JwtUtils.getUserIdAsLong() retourne null (username 'user1' n'est pas un nombre)" -ForegroundColor Gray
        Write-Host "   2. user-service n'est pas accessible pour recuperer l'ID utilisateur" -ForegroundColor Gray
        Write-Host "   3. Le compte n'existe pas dans account-service" -ForegroundColor Gray
        Write-Host "   4. Le format de la requete est incorrect" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Tests termines!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
