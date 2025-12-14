# VelocityX Platform - Health Check Script
# Verifies all services are running and healthy

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  VelocityX Platform - Health Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$services = @(
    @{Name="API Gateway"; URL="http://localhost:8080/actuator/health"; Port=8080},
    @{Name="Eureka Server"; URL="http://localhost:8761/actuator/health"; Port=8761},
    @{Name="User Service"; URL="http://localhost:8081/actuator/health"; Port=8081},
    @{Name="Wallet Service"; URL="http://localhost:8082/actuator/health"; Port=8082},
    @{Name="Transaction Service"; URL="http://localhost:8083/actuator/health"; Port=8083},
    @{Name="Reward Service"; URL="http://localhost:8084/actuator/health"; Port=8084},
    @{Name="Notification Service"; URL="http://localhost:8085/actuator/health"; Port=8085},
    @{Name="Fraud Detection"; URL="http://localhost:8086/actuator/health"; Port=8086}
)

$healthyCount = 0
$totalCount = $services.Count

foreach ($service in $services) {
    Write-Host "Checking $($service.Name)..." -ForegroundColor Yellow -NoNewline
    
    try {
        $response = Invoke-WebRequest -Uri $service.URL -TimeoutSec 5 -UseBasicParsing
        if ($response.StatusCode -eq 200) {
            Write-Host " [HEALTHY]" -ForegroundColor Green
            $healthyCount++
        } else {
            Write-Host " [UNHEALTHY] (Status: $($response.StatusCode))" -ForegroundColor Red
        }
    } catch {
        Write-Host " [DOWN] (Cannot connect)" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Summary" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Healthy Services: $healthyCount / $totalCount" -ForegroundColor $(if ($healthyCount -eq $totalCount) { "Green" } else { "Yellow" })
Write-Host ""

if ($healthyCount -eq $totalCount) {
    Write-Host "[OK] All services are healthy!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Platform is ready to use!" -ForegroundColor Green
} else {
    Write-Host "[WARNING] Some services are not healthy" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Troubleshooting:" -ForegroundColor Yellow
    Write-Host "1. Check docker-compose logs: docker-compose logs -f" -ForegroundColor White
    Write-Host "2. Restart unhealthy services: docker-compose restart [service-name]" -ForegroundColor White
    Write-Host "3. Check if ports are available" -ForegroundColor White
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Quick Links" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Eureka Dashboard:  http://localhost:8761" -ForegroundColor White
Write-Host "API Gateway:       http://localhost:8080" -ForegroundColor White
Write-Host ""
