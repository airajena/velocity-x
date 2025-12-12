package com.velocityx.fraud_detection_service.dto.response;

import com.velocityx.fraud_detection_service.enums.FraudCheckStatus;
import com.velocityx.fraud_detection_service.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCheckResponse {
    
    private String checkId;
    private String transactionId;
    private FraudCheckStatus status;
    private RiskLevel riskLevel;
    private Integer riskScore; // 0-100
    private String reason;
    private List<String> riskFactors;
    private Map<String, Object> metadata;
    private Instant checkedAt;
    private Boolean approved;
}
