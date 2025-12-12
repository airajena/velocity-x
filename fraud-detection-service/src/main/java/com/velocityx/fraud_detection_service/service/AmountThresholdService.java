package com.velocityx.fraud_detection_service.service;

import com.velocityx.fraud_detection_service.dto.RiskScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class AmountThresholdService {
    
    @Value("${fraud.rules.amount.high-risk-threshold:5000}")
    private BigDecimal highRiskThreshold;
    
    @Value("${fraud.rules.amount.medium-risk-threshold:1000}")
    private BigDecimal mediumRiskThreshold;
    
    @Value("${fraud.rules.amount.suspicious-threshold:10000}")
    private BigDecimal suspiciousThreshold;
    
    public void checkAmountThreshold(BigDecimal amount, RiskScore riskScore) {
        log.debug("Checking amount threshold for: {}", amount);
        
        if (amount.compareTo(suspiciousThreshold) > 0) {
            riskScore.addRiskFactor(
                    "AMOUNT_SUSPICIOUS",
                    String.format("Transaction amount %s exceeds suspicious threshold %s", amount, suspiciousThreshold),
                    40,
                    "CRITICAL"
            );
            log.warn("Suspicious amount detected: {}", amount);
        } else if (amount.compareTo(highRiskThreshold) > 0) {
            riskScore.addRiskFactor(
                    "AMOUNT_HIGH_RISK",
                    String.format("Transaction amount %s exceeds high risk threshold %s", amount, highRiskThreshold),
                    25,
                    "HIGH"
            );
            log.warn("High risk amount detected: {}", amount);
        } else if (amount.compareTo(mediumRiskThreshold) > 0) {
            riskScore.addRiskFactor(
                    "AMOUNT_MEDIUM_RISK",
                    String.format("Transaction amount %s exceeds medium risk threshold %s", amount, mediumRiskThreshold),
                    10,
                    "MEDIUM"
            );
            log.debug("Medium risk amount detected: {}", amount);
        }
    }
}
