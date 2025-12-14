package com.velocityx.fraud_detection_service.service;

import com.velocityx.fraud_detection_service.dto.RiskScore;
import com.velocityx.fraud_detection_service.dto.request.FraudCheckRequest;
import com.velocityx.fraud_detection_service.dto.response.FraudCheckResponse;
import com.velocityx.fraud_detection_service.entity.FraudCheck;
import com.velocityx.fraud_detection_service.enums.FraudCheckStatus;
import com.velocityx.fraud_detection_service.enums.RiskLevel;
import com.velocityx.fraud_detection_service.repository.FraudCheckRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {
    
    private final FraudCheckRepository fraudCheckRepository;
    private final VelocityCheckService velocityCheckService;
    private final AmountThresholdService amountThresholdService;
    private final GeolocationService geolocationService;
    private final DeviceFingerprintService deviceFingerprintService;
    private final BlacklistService blacklistService;
    private final RiskScoringService riskScoringService;
    private final FraudAlertService fraudAlertService;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public FraudCheckResponse performFraudCheck(FraudCheckRequest request) {
        log.info("Performing fraud check for transaction: {}", request.getTransactionId());
        
        // Check if already checked
        var existingCheck = fraudCheckRepository.findByTransactionId(request.getTransactionId());
        if (existingCheck.isPresent()) {
            log.warn("Fraud check already performed for transaction: {}", request.getTransactionId());
            return mapToResponse(existingCheck.get());
        }
        
        // Initialize risk score
        RiskScore riskScore = riskScoringService.calculateRiskScore();
        
        // Run all fraud detection rules
        velocityCheckService.checkVelocity(request.getUserId(), request.getAmount(), riskScore);
        amountThresholdService.checkAmountThreshold(request.getAmount(), riskScore);
        geolocationService.checkGeolocationAnomaly(
                request.getUserId(), 
                request.getLatitude(), 
                request.getLongitude(), 
                riskScore
        );
        deviceFingerprintService.checkDeviceFingerprint(
                request.getUserId(), 
                request.getDeviceId(), 
                request.getDeviceFingerprint(), 
                riskScore
        );
        blacklistService.checkBlacklist(
                request.getUserId(), 
                request.getIpAddress(), 
                request.getDeviceId(), 
                riskScore
        );
        
        // Calculate final risk level
        riskScore.setRiskLevel(riskScore.calculateRiskLevel());
        
        // Determine status
        FraudCheckStatus status = determineStatus(riskScore);
        
        // Save fraud check
        FraudCheck fraudCheck = saveFraudCheck(request, riskScore, status);
        
        // Send alert if needed
        if (riskScore.getRiskLevel() == RiskLevel.HIGH || riskScore.getRiskLevel() == RiskLevel.CRITICAL) {
            List<String> riskFactors = riskScore.getRiskFactors().stream()
                    .map(rf -> rf.getFactorType() + ": " + rf.getDescription())
                    .collect(Collectors.toList());
            
            fraudAlertService.sendFraudAlert(
                    request.getTransactionId(),
                    request.getUserId(),
                    riskScore.getRiskLevel(),
                    riskScore.getTotalScore(),
                    riskFactors,
                    riskScoringService.requiresManualReview(riskScore)
            );
        }
        
        log.info("Fraud check completed for transaction {}: Status={}, RiskLevel={}, Score={}", 
                request.getTransactionId(), status, riskScore.getRiskLevel(), riskScore.getTotalScore());
        
        return mapToResponse(fraudCheck);
    }
    
    private FraudCheckStatus determineStatus(RiskScore riskScore) {
        if (riskScoringService.shouldAutoReject(riskScore)) {
            return FraudCheckStatus.REJECTED;
        } else if (riskScoringService.shouldAutoApprove(riskScore)) {
            return FraudCheckStatus.APPROVED;
        } else {
            return FraudCheckStatus.MANUAL_REVIEW;
        }
    }
    
    private FraudCheck saveFraudCheck(FraudCheckRequest request, RiskScore riskScore, FraudCheckStatus status) {
        String riskFactorsJson = null;
        try {
            riskFactorsJson = objectMapper.writeValueAsString(riskScore.getRiskFactors());
        } catch (JsonProcessingException e) {
            log.error("Error serializing risk factors", e);
        }
        
        FraudCheck fraudCheck = FraudCheck.builder()
                .transactionId(request.getTransactionId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .status(status)
                .riskLevel(riskScore.getRiskLevel())
                .riskScore(riskScore.getTotalScore())
                .riskFactors(riskFactorsJson)
                .reason(generateReason(riskScore))
                .ipAddress(request.getIpAddress())
                .deviceId(request.getDeviceId())
                .deviceFingerprint(request.getDeviceFingerprint())
                .userAgent(request.getUserAgent())
                .country(request.getCountry())
                .city(request.getCity())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .checkedAt(Instant.now())
                .build();
        
        if (status == FraudCheckStatus.APPROVED) {
            fraudCheck.setApprovedAt(Instant.now());
        } else if (status == FraudCheckStatus.REJECTED) {
            fraudCheck.setRejectedAt(Instant.now());
        }
        
        return fraudCheckRepository.save(fraudCheck);
    }
    
    private String generateReason(RiskScore riskScore) {
        if (riskScore.getRiskFactors().isEmpty()) {
            return "No risk factors detected";
        }
        
        return riskScore.getRiskFactors().stream()
                .map(rf -> rf.getFactorType())
                .collect(Collectors.joining(", "));
    }
    
    private FraudCheckResponse mapToResponse(FraudCheck fraudCheck) {
        List<String> riskFactors = null;
        try {
            if (fraudCheck.getRiskFactors() != null) {
                List<RiskScore.RiskFactor> factors = objectMapper.readValue(
                        fraudCheck.getRiskFactors(), 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, RiskScore.RiskFactor.class)
                );
                riskFactors = factors.stream()
                .map(rf -> rf.getFactorType() + ": " + rf.getDescription())
                .collect(Collectors.toList());
            }
        } catch (JsonProcessingException e) {
            log.error("Error deserializing risk factors", e);
        }
        
        return FraudCheckResponse.builder()
                .checkId(fraudCheck.getId())
                .transactionId(fraudCheck.getTransactionId())
                .status(fraudCheck.getStatus())
                .riskLevel(fraudCheck.getRiskLevel())
                .riskScore(fraudCheck.getRiskScore())
                .reason(fraudCheck.getReason())
                .riskFactors(riskFactors)
                .checkedAt(fraudCheck.getCheckedAt())
                .approved(fraudCheck.getStatus() == FraudCheckStatus.APPROVED)
                .build();
    }
    
    public FraudCheckResponse getCheckByTransactionId(String transactionId) {
        return fraudCheckRepository.findByTransactionId(transactionId)
                .map(this::mapToResponse)
                .orElse(null);
    }
}
