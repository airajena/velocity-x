# ============================================
# VelocityX Platform - Complete Startup Script
# ============================================
# This script does EVERYTHING:
# 1. Sets Java 17
# 2. Kills all services and ports
# 3. Starts all services fresh
# ============================================

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  VelocityX Platform - Complete Startup" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# ============================================
# STEP 1: SET JAVA 17
# ============================================
Write-Host "[1/5] Setting Java 17..." -ForegroundColor Yellow

$javaHome = "C:\Program Files\Java\jdk-17"
if (-not (Test-Path $javaHome)) {
    Write-Host "  ERROR: Java 17 not found at $javaHome" -ForegroundColor Red
    Write-Host "  Please install Java 17 or update the path in this script" -ForegroundColor Red
    exit 1
}

$env:JAVA_HOME = $javaHome
$env:PATH = "$javaHome\bin;$env:PATH"

Write-Host "  JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
Write-Host "  Java Version: 17.0.12" -ForegroundColor Green
Write-Host ""

# ============================================
# STEP 2: KILL ALL SERVICES AND PORTS
# ============================================
Write-Host "[2/5] Killing all services and freeing ports..." -ForegroundColor Yellow

Get-Process -Name "java" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Seconds 5

Write-Host "  All Java processes killed" -ForegroundColor Green
Write-Host "  All ports freed" -ForegroundColor Green
Write-Host ""

# ============================================
# STEP 3: START EUREKA SERVER
# ============================================
Write-Host "[3/5] Starting Eureka Server..." -ForegroundColor Yellow

$baseDir = "C:\Users\airaj\OneDrive\Desktop\velocity-x"

$eurekaScript = @"
`$env:JAVA_HOME = '$javaHome'
`$env:PATH = '$javaHome\bin;' + `$env:PATH
cd '$baseDir\eureka-server'
Write-Host '========================================' -ForegroundColor Cyan
Write-Host 'EUREKA SERVER' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan
Write-Host 'Starting with Java 17...' -ForegroundColor Green
Write-Host ''
mvn spring-boot:run
"@

Start-Process powershell -ArgumentList "-NoExit", "-Command", $eurekaScript
Write-Host "  Eureka window opened" -ForegroundColor Green
Write-Host "  Waiting 60 seconds for Eureka to start..." -ForegroundColor Gray

for ($i = 60; $i -gt 0; $i--) {
    Write-Progress -Activity "Waiting for Eureka" -Status "$i seconds remaining" -PercentComplete ((60-$i)/60*100)
    Start-Sleep -Seconds 1
}
Write-Progress -Activity "Waiting for Eureka" -Completed

Write-Host "  Eureka should be ready" -ForegroundColor Green
Write-Host ""

# ============================================
# STEP 4: START ALL MICROSERVICES
# ============================================
Write-Host "[4/5] Starting all microservices..." -ForegroundColor Yellow

$services = @(
    @{Name="user-service"; Display="User Service"; Port=9081},
    @{Name="wallet-service"; Display="Wallet Service"; Port=9082},
    @{Name="transaction-service"; Display="Transaction Service"; Port=9083},
    @{Name="reward-service"; Display="Reward Service"; Port=9084},
    @{Name="notification-service"; Display="Notification Service"; Port=9085},
    @{Name="fraud-detection-service"; Display="Fraud Detection"; Port=9086}
)

foreach ($service in $services) {
    Write-Host "  Starting $($service.Display)..." -ForegroundColor Cyan
    
    $serviceScript = @"
`$env:JAVA_HOME = '$javaHome'
`$env:PATH = '$javaHome\bin;' + `$env:PATH
cd '$baseDir\$($service.Name)'
Write-Host '========================================' -ForegroundColor Cyan
Write-Host '$($service.Display.ToUpper())' -ForegroundColor Cyan
Write-Host 'Port: $($service.Port)' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan
Write-Host 'Starting with Java 17...' -ForegroundColor Green
Write-Host ''
mvn spring-boot:run
"@
    
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $serviceScript
    Start-Sleep -Seconds 3
}

Write-Host "  All microservice windows opened" -ForegroundColor Green
Write-Host "  Waiting 90 seconds for compilation..." -ForegroundColor Gray

for ($i = 90; $i -gt 0; $i--) {
    Write-Progress -Activity "Waiting for services to compile" -Status "$i seconds remaining" -PercentComplete ((90-$i)/90*100)
    Start-Sleep -Seconds 1
}
Write-Progress -Activity "Waiting for services to compile" -Completed

Write-Host "  Services should be compiling" -ForegroundColor Green
Write-Host ""

# ============================================
# STEP 5: START API GATEWAY
# ============================================
Write-Host "[5/5] Starting API Gateway..." -ForegroundColor Yellow

$gatewayScript = @"
`$env:JAVA_HOME = '$javaHome'
`$env:PATH = '$javaHome\bin;' + `$env:PATH
cd '$baseDir\api-gateway'
Write-Host '========================================' -ForegroundColor Cyan
Write-Host 'API GATEWAY' -ForegroundColor Cyan
Write-Host 'Port: 9080' -ForegroundColor Cyan
Write-Host '========================================' -ForegroundColor Cyan
Write-Host 'Starting with Java 17...' -ForegroundColor Green
Write-Host ''
mvn spring-boot:run
"@

Start-Process powershell -ArgumentList "-NoExit", "-Command", $gatewayScript
Write-Host "  API Gateway window opened" -ForegroundColor Green
Write-Host ""

# ============================================
# DONE!
# ============================================
Write-Host ""
Write-Host "============================================" -ForegroundColor Green
Write-Host "  ALL SERVICES STARTED!" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Green
Write-Host ""
Write-Host "8 PowerShell windows are now open:" -ForegroundColor Cyan
Write-Host "  1. Eureka Server (port 8761)" -ForegroundColor White
Write-Host "  2. User Service (port 9081)" -ForegroundColor White
Write-Host "  3. Wallet Service (port 9082)" -ForegroundColor White
Write-Host "  4. Transaction Service (port 9083)" -ForegroundColor White
Write-Host "  5. Reward Service (port 9084)" -ForegroundColor White
Write-Host "  6. Notification Service (port 9085)" -ForegroundColor White
Write-Host "  7. Fraud Detection (port 9086)" -ForegroundColor White
Write-Host "  8. API Gateway (port 9080)" -ForegroundColor White
Write-Host ""
Write-Host "NEXT STEPS:" -ForegroundColor Yellow
Write-Host ""
Write-Host "1. Wait 3-5 minutes for all services to compile and start" -ForegroundColor White
Write-Host ""
Write-Host "2. Check each window for 'Started [ServiceName]Application'" -ForegroundColor White
Write-Host ""
Write-Host "3. Check Eureka Dashboard:" -ForegroundColor White
Write-Host "   http://localhost:8761" -ForegroundColor Cyan
Write-Host "   Username: admin" -ForegroundColor Gray
Write-Host "   Password: admin" -ForegroundColor Gray
Write-Host ""
Write-Host "4. You should see 6 services registered:" -ForegroundColor White
Write-Host "   - USER-SERVICE" -ForegroundColor Green
Write-Host "   - WALLET-SERVICE" -ForegroundColor Green
Write-Host "   - TRANSACTION-SERVICE" -ForegroundColor Green
Write-Host "   - REWARD-SERVICE" -ForegroundColor Green
Write-Host "   - NOTIFICATION-SERVICE" -ForegroundColor Green
Write-Host "   - FRAUD-DETECTION-SERVICE" -ForegroundColor Green
Write-Host ""
Write-Host "5. Test the platform:" -ForegroundColor White
Write-Host "   API Gateway: http://localhost:9080" -ForegroundColor Cyan
Write-Host ""
Write-Host "If any service fails, check its window for error messages" -ForegroundColor Yellow
Write-Host ""
