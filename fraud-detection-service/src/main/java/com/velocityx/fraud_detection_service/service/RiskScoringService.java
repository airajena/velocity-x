package com.velocityx.fraud_detection_service.service;

import com.velocityx.fraud_detection_service.dto.RiskScore;
import com.velocityx.fraud_detection_service.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskScoringService {
    
    public RiskScore calculateRiskScore() {
        return RiskScore.builder()
                .totalScore(0)
                .riskLevel(RiskLevel.LOW)
                .build();
    }
    
    public RiskLevel determineRiskLevel(Integer totalScore) {
        if (totalScore >= 80) {
            return RiskLevel.CRITICAL;
        } else if (totalScore >= 50) {
            return RiskLevel.HIGH;
        } else if (totalScore >= 20) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
    
    public boolean shouldAutoApprove(RiskScore riskScore) {
        return riskScore.getRiskLevel() == RiskLevel.LOW;
    }
    
    public boolean shouldAutoReject(RiskScore riskScore) {
        return riskScore.getRiskLevel() == RiskLevel.CRITICAL;
    }
    
    public boolean requiresManualReview(RiskScore riskScore) {
        return riskScore.getRiskLevel() == RiskLevel.HIGH || 
               riskScore.getRiskLevel() == RiskLevel.MEDIUM;
    }
}
