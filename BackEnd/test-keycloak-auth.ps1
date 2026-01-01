# Script de diagnostic pour l'authentification Keycloak

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Diagnostic Authentification Keycloak" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$keycloakUrl = "http://localhost:8080"
$realm = "ebanking-realm"
$clientId = "ebanking-client"

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  Keycloak URL: $keycloakUrl" -ForegroundColor Gray
Write-Host "  Realm: $realm" -ForegroundColor Gray
Write-Host "  Client ID: $clientId" -ForegroundColor Gray
Write-Host ""

# Test 1: Verifier que Keycloak est accessible
Write-Host "Test 1: Verification de l'accessibilite de Keycloak" -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "$keycloakUrl/realms/$realm" -Method GET -TimeoutSec 5 -ErrorAction Stop
    Write-Host "OK - Keycloak est accessible" -ForegroundColor Green
    Write-Host "  Status: $($response.StatusCode)" -ForegroundColor Gray
} catch {
    Write-Host "ERREUR - Keycloak n'est pas accessible" -ForegroundColor Red
    Write-Host "  Message: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host ""
    Write-Host "Verifiez que Keycloak est demarre sur le port 8080" -ForegroundColor Yellow
    exit 1
}
Write-Host ""

# Test 2: Essayer sans client secret (si le client est public)
Write-Host "Test 2: Authentification sans client secret (client public)" -ForegroundColor Cyan
$tokenUrl = "$keycloakUrl/realms/$realm/protocol/openid-connect/token"
$body = @{
    grant_type = "password"
    client_id = $clientId
    username = "client1"
    password = "password"
}

try {
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
    $token = $tokenResponse.access_token
    Write-Host "OK - Authentification reussie SANS client secret!" -ForegroundColor Green
    Write-Host "  Le client est public, pas besoin de client_secret" -ForegroundColor Green
    Write-Host "  Token obtenu: $($token.Substring(0, [Math]::Min(50, $token.Length)))..." -ForegroundColor Gray
    exit 0
} catch {
    Write-Host "ERREUR - Authentification echouee sans client secret" -ForegroundColor Yellow
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "  Reponse: $responseBody" -ForegroundColor Gray
    }
}
Write-Host ""

# Test 3: Essayer avec client secret vide
Write-Host "Test 3: Authentification avec client secret vide" -ForegroundColor Cyan
$body = @{
    grant_type = "password"
    client_id = $clientId
    client_secret = ""
    username = "client1"
    password = "password"
}

try {
    $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
    $token = $tokenResponse.access_token
    Write-Host "OK - Authentification reussie avec client secret vide!" -ForegroundColor Green
    Write-Host "  Token obtenu: $($token.Substring(0, [Math]::Min(50, $token.Length)))..." -ForegroundColor Gray
    exit 0
} catch {
    Write-Host "ERREUR - Authentification echouee avec client secret vide" -ForegroundColor Yellow
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "  Reponse: $responseBody" -ForegroundColor Gray
    }
}
Write-Host ""

# Test 4: Essayer avec differentes valeurs de client secret
Write-Host "Test 4: Authentification avec differentes valeurs de client secret" -ForegroundColor Cyan
$secrets = @("your-client-secret", "ebanking-secret", "secret", "client-secret", "")

foreach ($secret in $secrets) {
    Write-Host "  Essai avec client_secret: '$secret'" -ForegroundColor Gray
    $body = @{
        grant_type = "password"
        client_id = $clientId
        username = "client1"
        password = "password"
    }
    
    if ($secret -ne "") {
        $body["client_secret"] = $secret
    }
    
    try {
        $tokenResponse = Invoke-RestMethod -Uri $tokenUrl -Method Post -Body $body -ContentType "application/x-www-form-urlencoded"
        $token = $tokenResponse.access_token
        Write-Host "  OK - Authentification reussie avec client_secret: '$secret'!" -ForegroundColor Green
        Write-Host ""
        Write-Host "========================================" -ForegroundColor Green
        Write-Host "Solution trouvee!" -ForegroundColor Green
        Write-Host "========================================" -ForegroundColor Green
        Write-Host ""
        Write-Host "Utilisez cette configuration dans test-integration-simple.ps1:" -ForegroundColor Yellow
        if ($secret -eq "") {
            Write-Host "  Supprimez la ligne client_secret du body" -ForegroundColor Yellow
        } else {
            Write-Host "  `$clientSecret = `"$secret`"" -ForegroundColor Yellow
        }
        exit 0
    } catch {
        # Continue avec le prochain secret
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Red
Write-Host "Aucune configuration fonctionnelle trouvee" -ForegroundColor Red
Write-Host "========================================" -ForegroundColor Red
Write-Host ""
Write-Host "Prochaines etapes:" -ForegroundColor Yellow
Write-Host "1. Verifiez dans Keycloak Admin Console:" -ForegroundColor Yellow
Write-Host "   - Realm: $realm" -ForegroundColor Gray
Write-Host "   - Client: $clientId" -ForegroundColor Gray
Write-Host "   - Access Type: public ou confidential" -ForegroundColor Gray
Write-Host "   - Si confidential, recuperez le client secret" -ForegroundColor Gray
Write-Host ""
Write-Host "2. Verifiez que l'utilisateur existe:" -ForegroundColor Yellow
Write-Host "   - Username: client1" -ForegroundColor Gray
Write-Host "   - Password: password" -ForegroundColor Gray
Write-Host ""
Write-Host "3. Accedez a Keycloak Admin Console:" -ForegroundColor Yellow
Write-Host "   http://localhost:8080/admin" -ForegroundColor Gray
Write-Host "   Username: admin" -ForegroundColor Gray
Write-Host "   Password: admin" -ForegroundColor Gray
