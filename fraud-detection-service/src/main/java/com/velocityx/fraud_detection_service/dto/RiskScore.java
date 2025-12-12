package com.velocityx.fraud_detection_service.dto;

import com.velocityx.fraud_detection_service.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScore {
    
    private Integer totalScore; // 0-100
    private RiskLevel riskLevel;
    
    @Builder.Default
    private List<RiskFactor> riskFactors = new ArrayList<>();
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskFactor {
        private String factorType;
        private String description;
        private Integer scoreImpact;
        private String severity;
    }
    
    public void addRiskFactor(String factorType, String description, Integer scoreImpact, String severity) {
        if (riskFactors == null) {
            riskFactors = new ArrayList<>();
        }
        riskFactors.add(RiskFactor.builder()
                .factorType(factorType)
                .description(description)
                .scoreImpact(scoreImpact)
                .severity(severity)
                .build());
        this.totalScore = (this.totalScore == null ? 0 : this.totalScore) + scoreImpact;
    }
    
    public RiskLevel calculateRiskLevel() {
        if (totalScore >= 80) return RiskLevel.CRITICAL;
        if (totalScore >= 50) return RiskLevel.HIGH;
        if (totalScore >= 20) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }
}
