# Script pour installer security-common dans le repository Maven local
# Ce script doit être exécuté avant docker-compose build

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "BUILD SECURITY-COMMON MODULE" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$securityCommonPath = Join-Path $PSScriptRoot "security-common"

if (-not (Test-Path $securityCommonPath)) {
    Write-Host "ERREUR: Le répertoire security-common n'existe pas!" -ForegroundColor Red
    exit 1
}

Write-Host "Installation de security-common dans le repository Maven local..." -ForegroundColor Yellow
Write-Host ""

try {
    Push-Location $securityCommonPath
    mvn clean install -DskipTests
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ security-common installé avec succès!" -ForegroundColor Green
        Write-Host ""
        Write-Host "Vous pouvez maintenant lancer: docker-compose build" -ForegroundColor Cyan
    } else {
        Write-Host "❌ Erreur lors de l'installation de security-common" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "ERREUR: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
} finally {
    Pop-Location
}
