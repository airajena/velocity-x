package com.velocityx.reward_service.controller;

import com.velocityx.reward_service.dto.request.*;
import com.velocityx.reward_service.dto.response.BalanceResponse;
import com.velocityx.reward_service.dto.response.TransactionResponse;
import com.velocityx.reward_service.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Reward Management", description = "Double-Entry Ledger Reward System APIs")
@SecurityRequirement(name = "bearer-jwt")
public class RewardController {
    
    private final RewardService rewardService;
    
    @PostMapping("/earn")
    @Operation(summary = "Earn reward points", description = "User earns reward points")
    public ResponseEntity<TransactionResponse> earn(@Valid @RequestBody EarnRequest request) {
        log.info("REST request to earn rewards: userId={}, amount={}", request.getUserId(), request.getAmount());
        TransactionResponse response = rewardService.earn(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @PostMapping("/hold")
    @Operation(summary = "Hold reward points", description = "Reserve points for redemption")
    public ResponseEntity<TransactionResponse> hold(@Valid @RequestBody HoldRequest request) {
        log.info("REST request to hold rewards: userId={}, amount={}", request.getUserId(), request.getAmount());
        TransactionResponse response = rewardService.hold(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @PostMapping("/capture")
    @Operation(summary = "Capture held points", description = "Finalize reserved hold and convert to redemption")
    public ResponseEntity<TransactionResponse> capture(@Valid @RequestBody CaptureRequest request) {
        log.info("REST request to capture rewards: transactionId={}", request.getTransactionId());
        TransactionResponse response = rewardService.capture(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @PostMapping("/release")
    @Operation(summary = "Release held points", description = "Release a previously held reservation")
    public ResponseEntity<TransactionResponse> release(@Valid @RequestBody ReleaseRequest request) {
        log.info("REST request to release rewards: transactionId={}", request.getTransactionId());
        TransactionResponse response = rewardService.release(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Get user balance", description = "Get balances and recent ledger")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable Long userId) {
        log.info("REST request to get balance: userId={}", userId);
        BalanceResponse response = rewardService.getBalance(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get transaction", description = "Get transaction details")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        log.info("REST request to get transaction: {}", transactionId);
        TransactionResponse response = rewardService.getTransaction(transactionId);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/admin/adjust")
    @Operation(summary = "Admin adjustment", description = "Admin-only credit or debit adjustment")
    @PreAuthorize("hasRole('ROLE_REWARDS_ADMIN')")
    public ResponseEntity<TransactionResponse> adminAdjust(@Valid @RequestBody AdminAdjustRequest request) {
        log.info("REST request for admin adjustment: userId={}, amount={}", request.getUserId(), request.getAmount());
        TransactionResponse response = rewardService.adminAdjust(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
