package com.velocityx.fraud_detection_service.dto.event;

import com.velocityx.fraud_detection_service.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {
    
    private String alertId;
    private String transactionId;
    private String userId;
    private RiskLevel riskLevel;
    private Integer riskScore;
    private String alertType;
    private String message;
    private List<String> riskFactors;
    private Instant alertedAt;
    private Boolean requiresManualReview;
}
