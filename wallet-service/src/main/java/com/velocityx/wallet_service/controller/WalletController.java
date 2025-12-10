package com.velocityx.wallet_service.controller;

import com.velocityx.wallet_service.dto.request.*;
import com.velocityx.wallet_service.dto.response.TransactionResponse;
import com.velocityx.wallet_service.dto.response.WalletResponse;
import com.velocityx.wallet_service.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet Management", description = "PayPal-style Wallet APIs with Double-Entry Ledger")
@SecurityRequirement(name = "bearer-jwt")
public class WalletController {
    
    private final WalletService walletService;
    
    @PostMapping
    @Operation(summary = "Create wallet", description = "Create a new wallet for user")
    public ResponseEntity<WalletResponse> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        log.info("REST request to create wallet: userId={}", request.getUserId());
        WalletResponse response = walletService.createWallet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/credit")
    @Operation(summary = "Credit wallet", description = "Add funds to wallet")
    public ResponseEntity<TransactionResponse> credit(@Valid @RequestBody CreditRequest request) {
        log.info("REST request to credit wallet: userId={}, amount={}", request.getUserId(), request.getAmount());
        TransactionResponse response = walletService.credit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @PostMapping("/debit")
    @Operation(summary = "Debit wallet", description = "Deduct funds from wallet")
    public ResponseEntity<TransactionResponse> debit(@Valid @RequestBody DebitRequest request) {
        log.info("REST request to debit wallet: userId={}, amount={}", request.getUserId(), request.getAmount());
        TransactionResponse response = walletService.debit(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @PostMapping("/hold")
    @Operation(summary = "Hold funds", description = "Reserve funds for later capture")
    public ResponseEntity<TransactionResponse> hold(@Valid @RequestBody HoldRequest request) {
        log.info("REST request to hold funds: userId={}, amount={}", request.getUserId(), request.getAmount());
        TransactionResponse response = walletService.hold(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @PostMapping("/capture")
    @Operation(summary = "Capture held funds", description = "Finalize hold and deduct funds")
    public ResponseEntity<TransactionResponse> capture(@Valid @RequestBody CaptureRequest request) {
        log.info("REST request to capture hold: holdTxnId={}", request.getHoldTransactionId());
        TransactionResponse response = walletService.capture(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @PostMapping("/release/{holdTransactionId}")
    @Operation(summary = "Release held funds", description = "Release hold and return to available balance")
    public ResponseEntity<TransactionResponse> release(@PathVariable String holdTransactionId) {
        log.info("REST request to release hold: holdTxnId={}", holdTransactionId);
        TransactionResponse response = walletService.release(holdTransactionId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds", description = "Transfer funds between wallets")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("REST request to transfer: from={}, to={}, amount={}", 
                request.getFromUserId(), request.getToUserId(), request.getAmount());
        TransactionResponse response = walletService.transfer(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get wallet by user", description = "Get wallet details for user")
    public ResponseEntity<WalletResponse> getWallet(@PathVariable Long userId) {
        log.info("REST request to get wallet: userId={}", userId);
        WalletResponse response = walletService.getWallet(userId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{walletId}")
    @Operation(summary = "Get wallet", description = "Get wallet by wallet ID")
    public ResponseEntity<WalletResponse> getWalletByWalletId(@PathVariable String walletId) {
        log.info("REST request to get wallet: walletId={}", walletId);
        WalletResponse response = walletService.getWalletByWalletId(walletId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/transaction/{transactionId}")
    @Operation(summary = "Get transaction", description = "Get transaction details")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        log.info("REST request to get transaction: {}", transactionId);
        TransactionResponse response = walletService.getTransaction(transactionId);
        return ResponseEntity.ok(response);
    }
}
