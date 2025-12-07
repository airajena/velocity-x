package com.velocityx.transaction_service.controller;

import com.velocityx.transaction_service.dto.request.*;
import com.velocityx.transaction_service.dto.response.PagedResponse;
import com.velocityx.transaction_service.dto.response.TransactionResponse;
import com.velocityx.transaction_service.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Management", description = "APIs for managing transactions")
@SecurityRequirement(name = "bearer-jwt")
public class TransactionController {
    
    private final TransactionService transactionService;
    
    @PostMapping
    @Operation(summary = "Create a new transaction", description = "Creates a new transaction with the specified details")
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request) {
        log.info("REST request to create transaction: {}", request);
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/hold")
    @Operation(summary = "Hold funds", description = "Reserve funds without deducting (PayPal-style pre-authorization)")
    public ResponseEntity<TransactionResponse> holdFunds(
            @Valid @RequestBody HoldFundsRequest request) {
        log.info("REST request to hold funds: {}", request);
        TransactionResponse response = transactionService.holdFunds(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/{id}/capture")
    @Operation(summary = "Capture held funds", description = "Convert held funds to actual debit")
    public ResponseEntity<TransactionResponse> captureFunds(
            @PathVariable Long id,
            @Valid @RequestBody CaptureFundsRequest request) {
        log.info("REST request to capture funds for transaction: {}", id);
        TransactionResponse response = transactionService.captureFunds(id, request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel transaction", description = "Cancel a pending or held transaction")
    public ResponseEntity<TransactionResponse> cancelTransaction(@PathVariable Long id) {
        log.info("REST request to cancel transaction: {}", id);
        TransactionResponse response = transactionService.cancelTransaction(id);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/{id}/refund")
    @Operation(summary = "Refund transaction", description = "Refund a completed transaction")
    public ResponseEntity<TransactionResponse> refundTransaction(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequest request) {
        log.info("REST request to refund transaction: {}", id);
        TransactionResponse response = transactionService.refundTransaction(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/transfer")
    @Operation(summary = "Transfer funds", description = "Transfer funds between wallets")
    public ResponseEntity<TransactionResponse> transferFunds(
            @Valid @RequestBody TransferRequest request) {
        log.info("REST request to transfer funds: {}", request);
        TransactionResponse response = transactionService.transferFunds(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get transaction by ID", description = "Retrieve transaction details by ID")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable Long id) {
        log.info("REST request to get transaction: {}", id);
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/txn/{transactionId}")
    @Operation(summary = "Get transaction by transaction ID", description = "Retrieve transaction details by transaction ID")
    public ResponseEntity<TransactionResponse> getTransactionByTransactionId(
            @PathVariable String transactionId) {
        log.info("REST request to get transaction by transactionId: {}", transactionId);
        TransactionResponse response = transactionService.getTransactionByTransactionId(transactionId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user transactions", description = "Retrieve all transactions for a specific user")
    public ResponseEntity<PagedResponse<TransactionResponse>> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("REST request to get transactions for user: {}", userId);
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PagedResponse<TransactionResponse> response = 
                transactionService.getUserTransactions(userId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all transactions", description = "Retrieve all transactions (Admin only)")
    public ResponseEntity<PagedResponse<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("REST request to get all transactions");
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PagedResponse<TransactionResponse> response = 
                transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(response);
    }
}
