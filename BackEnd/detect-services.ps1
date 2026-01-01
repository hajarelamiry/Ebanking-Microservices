# Script pour detecter les ports actifs des services

Write-Host "Detection des services..." -ForegroundColor Cyan
Write-Host ""

# Ports possibles pour Keycloak
$keycloakPorts = @(8180, 8080, 9090, 8081)

# Ports possibles pour Account Service
$accountPorts = @(8087, 8083, 8082)

# Ports possibles pour Crypto Service
$cryptoPorts = @(8085, 8084)

# Ports possibles pour Eureka
$eurekaPorts = @(8761, 8762)

Write-Host "Test Keycloak..." -ForegroundColor Yellow
foreach ($port in $keycloakPorts) {
    $url = "http://localhost:$port/realms/ebanking-realm"
    try {
        $response = Invoke-WebRequest -Uri $url -Method GET -TimeoutSec 1 -ErrorAction Stop
        Write-Host "  OK - Keycloak trouve sur le port $port" -ForegroundColor Green
        $keycloakPort = $port
        break
    } catch {
        # Continue
    }
}

Write-Host ""
Write-Host "Test Account Service..." -ForegroundColor Yellow
foreach ($port in $accountPorts) {
    $url = "http://localhost:$port/api/accounts"
    try {
        $response = Invoke-WebRequest -Uri $url -Method GET -TimeoutSec 1 -ErrorAction Stop
        Write-Host "  OK - Account Service trouve sur le port $port" -ForegroundColor Green
        $accountPort = $port
        break
    } catch {
        # Continue
    }
}

Write-Host ""
Write-Host "Test Crypto Service..." -ForegroundColor Yellow
foreach ($port in $cryptoPorts) {
    $url = "http://localhost:$port/api/v1/crypto/prices"
    try {
        $response = Invoke-WebRequest -Uri $url -Method GET -TimeoutSec 1 -ErrorAction Stop
        Write-Host "  OK - Crypto Service trouve sur le port $port" -ForegroundColor Green
        $cryptoPort = $port
        break
    } catch {
        # Continue
    }
}

Write-Host ""
Write-Host "Test Eureka..." -ForegroundColor Yellow
foreach ($port in $eurekaPorts) {
    $url = "http://localhost:$port"
    try {
        $response = Invoke-WebRequest -Uri $url -Method GET -TimeoutSec 1 -ErrorAction Stop
        Write-Host "  OK - Eureka trouve sur le port $port" -ForegroundColor Green
        $eurekaPort = $port
        break
    } catch {
        # Continue
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Resume:" -ForegroundColor Cyan
if ($keycloakPort) {
    Write-Host "  Keycloak: http://localhost:$keycloakPort" -ForegroundColor Green
} else {
    Write-Host "  Keycloak: NON TROUVE" -ForegroundColor Red
}

if ($accountPort) {
    Write-Host "  Account Service: http://localhost:$accountPort" -ForegroundColor Green
} else {
    Write-Host "  Account Service: NON TROUVE" -ForegroundColor Red
}

if ($cryptoPort) {
    Write-Host "  Crypto Service: http://localhost:$cryptoPort" -ForegroundColor Green
} else {
    Write-Host "  Crypto Service: NON TROUVE" -ForegroundColor Red
}

if ($eurekaPort) {
    Write-Host "  Eureka: http://localhost:$eurekaPort" -ForegroundColor Green
} else {
    Write-Host "  Eureka: NON TROUVE" -ForegroundColor Yellow
}

Write-Host "========================================" -ForegroundColor Cyan
