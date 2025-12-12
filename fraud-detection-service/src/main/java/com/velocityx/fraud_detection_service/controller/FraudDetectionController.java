package com.velocityx.fraud_detection_service.controller;

import com.velocityx.fraud_detection_service.dto.request.FraudCheckRequest;
import com.velocityx.fraud_detection_service.dto.response.FraudCheckResponse;
import com.velocityx.fraud_detection_service.service.FraudDetectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fraud")
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionController {
    
    private final FraudDetectionService fraudDetectionService;
    
    @PostMapping("/check")
    public ResponseEntity<FraudCheckResponse> checkFraud(@Valid @RequestBody FraudCheckRequest request) {
        log.info("Fraud check requested for transaction: {}", request.getTransactionId());
        FraudCheckResponse response = fraudDetectionService.performFraudCheck(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/check/{transactionId}")
    public ResponseEntity<FraudCheckResponse> getCheckByTransactionId(@PathVariable String transactionId) {
        FraudCheckResponse response = fraudDetectionService.getCheckByTransactionId(transactionId);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}
