# Test VelocityX Platform - Live Testing
# This will test all services end-to-end

$ErrorActionPreference = "Continue"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  VelocityX Platform - Live Testing" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$apiGateway = "http://localhost:9080"
$testEmail = "test@velocityx.com"
$testPassword = "Test@123"

# Test 1: Check Eureka
Write-Host "Test 1: Checking Eureka Dashboard..." -ForegroundColor Yellow
try {
    $eurekaUser = "admin"
    $eurekaPass = "admin"
    $pair = "$($eurekaUser):$($eurekaPass)"
    $encodedCreds = [System.Convert]::ToBase64String([System.Text.Encoding]::ASCII.GetBytes($pair))
    $headers = @{ Authorization = "Basic $encodedCreds" }
    
    $eureka = Invoke-WebRequest -Uri "http://localhost:8761" -Headers $headers -UseBasicParsing -TimeoutSec 5
    Write-Host "  [PASS] Eureka is UP" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] Eureka is DOWN" -ForegroundColor Red
    Write-Host "  Cannot proceed without Eureka" -ForegroundColor Red
    exit 1
}

# Test 2: Check API Gateway
Write-Host ""
Write-Host "Test 2: Checking API Gateway..." -ForegroundColor Yellow
try {
    $gateway = Invoke-WebRequest -Uri "$apiGateway/actuator/health" -UseBasicParsing -TimeoutSec 5
    Write-Host "  [PASS] API Gateway is UP" -ForegroundColor Green
} catch {
    Write-Host "  [FAIL] API Gateway is DOWN" -ForegroundColor Red
}

# Test 3: User Registration
Write-Host ""
Write-Host "Test 3: User Registration..." -ForegroundColor Yellow
$signupBody = @{
    email = $testEmail
    password = $testPassword
    firstName = "Test"
    lastName = "User"
    phoneNumber = "+1234567890"
} | ConvertTo-Json

try {
    $signup = Invoke-RestMethod -Uri "$apiGateway/api/users/auth/signup" `
        -Method POST `
        -Body $signupBody `
        -ContentType "application/json" `
        -TimeoutSec 10
    
    Write-Host "  [PASS] User registered successfully" -ForegroundColor Green
    Write-Host "  User ID: $($signup.userId)" -ForegroundColor Cyan
} catch {
    if ($_.Exception.Response.StatusCode -eq 409) {
        Write-Host "  [INFO] User already exists (OK)" -ForegroundColor Yellow
    } else {
        Write-Host "  [FAIL] Registration failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

# Test 4: User Login
Write-Host ""
Write-Host "Test 4: User Login..." -ForegroundColor Yellow
$loginBody = @{
    email = $testEmail
    password = $testPassword
} | ConvertTo-Json

try {
    $login = Invoke-RestMethod -Uri "$apiGateway/api/users/auth/login" `
        -Method POST `
        -Body $loginBody `
        -ContentType "application/json" `
        -TimeoutSec 10
    
    $token = $login.accessToken
    Write-Host "  [PASS] Login successful" -ForegroundColor Green
    Write-Host "  Token: $($token.Substring(0, 20))..." -ForegroundColor Cyan
} catch {
    Write-Host "  [FAIL] Login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "  Cannot proceed without authentication" -ForegroundColor Red
    exit 1
}

# Test 5: Get User Profile
Write-Host ""
Write-Host "Test 5: Get User Profile..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $token"
}

try {
    $profile = Invoke-RestMethod -Uri "$apiGateway/api/users/me" `
        -Method GET `
        -Headers $headers `
        -TimeoutSec 10
    
    Write-Host "  [PASS] Profile retrieved" -ForegroundColor Green
    Write-Host "  Name: $($profile.firstName) $($profile.lastName)" -ForegroundColor Cyan
    Write-Host "  Email: $($profile.email)" -ForegroundColor Cyan
} catch {
    Write-Host "  [FAIL] Profile retrieval failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 6: Get Wallet
Write-Host ""
Write-Host "Test 6: Get Wallet..." -ForegroundColor Yellow
try {
    $wallet = Invoke-RestMethod -Uri "$apiGateway/api/wallets/my-wallet" `
        -Method GET `
        -Headers $headers `
        -TimeoutSec 10
    
    Write-Host "  [PASS] Wallet retrieved" -ForegroundColor Green
    Write-Host "  Balance: $($wallet.balance) $($wallet.currency)" -ForegroundColor Cyan
    Write-Host "  Status: $($wallet.status)" -ForegroundColor Cyan
} catch {
    Write-Host "  [FAIL] Wallet retrieval failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 7: Add Funds
Write-Host ""
Write-Host "Test 7: Add Funds to Wallet..." -ForegroundColor Yellow
$addFundsBody = @{
    amount = 1000.00
    currency = "USD"
    description = "Test deposit"
} | ConvertTo-Json

try {
    $deposit = Invoke-RestMethod -Uri "$apiGateway/api/wallets/deposit" `
        -Method POST `
        -Headers $headers `
        -Body $addFundsBody `
        -ContentType "application/json" `
        -TimeoutSec 10
    
    Write-Host "  [PASS] Funds added successfully" -ForegroundColor Green
    Write-Host "  New Balance: $($deposit.balance) $($deposit.currency)" -ForegroundColor Cyan
} catch {
    Write-Host "  [FAIL] Add funds failed: $($_.Exception.Message)" -ForegroundColor Red
}

# Test 8: Check Transaction History
Write-Host ""
Write-Host "Test 8: Get Transaction History..." -ForegroundColor Yellow
try {
    $transactions = Invoke-RestMethod -Uri "$apiGateway/api/transactions?page=0&size=10" `
        -Method GET `
        -Headers $headers `
        -TimeoutSec 10
    
    Write-Host "  [PASS] Transactions retrieved" -ForegroundColor Green
    Write-Host "  Total Transactions: $($transactions.totalElements)" -ForegroundColor Cyan
} catch {
    Write-Host "  [FAIL] Transaction retrieval failed: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Testing Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  Check the results above" -ForegroundColor White
Write-Host "  All [PASS] means platform is working!" -ForegroundColor Green
Write-Host ""
Write-Host "Next: Check Eureka Dashboard" -ForegroundColor Yellow
Write-Host "  http://localhost:8761" -ForegroundColor Cyan
Write-Host "  (admin / admin)" -ForegroundColor Gray
Write-Host ""
