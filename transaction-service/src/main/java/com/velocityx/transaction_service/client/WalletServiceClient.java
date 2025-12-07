package com.velocityx.transaction_service.client;

import com.velocityx.transaction_service.client.dto.WalletResponse;
import com.velocityx.transaction_service.client.dto.DebitRequest;
import com.velocityx.transaction_service.client.dto.CreditRequest;
import com.velocityx.transaction_service.client.dto.HoldRequest;
import com.velocityx.transaction_service.client.dto.CaptureRequest;
import com.velocityx.transaction_service.client.dto.HoldResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "wallet-service",
        url = "${app.services.wallet-service.url}",
        configuration = com.velocityx.transaction_service.config.FeignConfig.class
)
public interface WalletServiceClient {
    
    @PostMapping("/api/v1/wallets/debit")
    @CircuitBreaker(name = "walletService", fallbackMethod = "debitFallback")
    @Retry(name = "walletService")
    WalletResponse debit(@RequestBody DebitRequest request);
    
    @PostMapping("/api/v1/wallets/credit")
    @CircuitBreaker(name = "walletService", fallbackMethod = "creditFallback")
    @Retry(name = "walletService")
    WalletResponse credit(@RequestBody CreditRequest request);
    
    @PostMapping("/api/v1/wallets/hold")
    @CircuitBreaker(name = "walletService", fallbackMethod = "holdFallback")
    @Retry(name = "walletService")
    HoldResponse placeHold(@RequestBody HoldRequest request);
    
    @PostMapping("/api/v1/wallets/capture")
    @CircuitBreaker(name = "walletService", fallbackMethod = "captureFallback")
    @Retry(name = "walletService")
    WalletResponse capture(@RequestBody CaptureRequest request);
    
    @PostMapping("/api/v1/wallets/release/{holdReference}")
    @CircuitBreaker(name = "walletService")
    @Retry(name = "walletService")
    HoldResponse release(@PathVariable String holdReference);
    
    @GetMapping("/api/v1/wallets/{userId}")
    @CircuitBreaker(name = "walletService")
    @Retry(name = "walletService")
    WalletResponse getWallet(@PathVariable Long userId);
    
    // Fallback methods
    default WalletResponse debitFallback(DebitRequest request, Exception ex) {
        throw new RuntimeException("Wallet service is unavailable. Please try again later.", ex);
    }
    
    default WalletResponse creditFallback(CreditRequest request, Exception ex) {
        throw new RuntimeException("Wallet service is unavailable. Please try again later.", ex);
    }
    
    default HoldResponse holdFallback(HoldRequest request, Exception ex) {
        throw new RuntimeException("Wallet service is unavailable. Please try again later.", ex);
    }
    
    default WalletResponse captureFallback(CaptureRequest request, Exception ex) {
        throw new RuntimeException("Wallet service is unavailable. Please try again later.", ex);
    }
}
