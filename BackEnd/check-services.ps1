Write-Host "Vérification des services..." -ForegroundColor Cyan

$services = @(
    @{Name="Audit Service"; Url="http://localhost:8084/api/audit/log"; Method="POST"},
    @{Name="Payment Service"; Url="http://localhost:8086/api/v1/payments"; Method="GET"},
    @{Name="Crypto Service"; Url="http://localhost:8085/api/v1/crypto/prices"; Method="GET"}
)

foreach ($service in $services) {
    try {
        $response = Invoke-WebRequest -Uri $service.Url -Method $service.Method -TimeoutSec 2 -ErrorAction Stop
        Write-Host "$($service.Name) : OK (Status: $($response.StatusCode))" -ForegroundColor Green
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode) {
            Write-Host "$($service.Name) : Répond (Status: $statusCode)" -ForegroundColor Yellow
        } else {
            Write-Host "$($service.Name) : ERREUR - Service non accessible" -ForegroundColor Red
        }
    }
}
