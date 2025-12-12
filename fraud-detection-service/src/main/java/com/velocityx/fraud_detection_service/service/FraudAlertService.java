package com.velocityx.fraud_detection_service.service;

import com.velocityx.fraud_detection_service.dto.event.FraudAlert;
import com.velocityx.fraud_detection_service.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAlertService {
    
    private final KafkaTemplate<String, FraudAlert> kafkaTemplate;
    
    public void sendFraudAlert(String transactionId, String userId, RiskLevel riskLevel, 
                               Integer riskScore, List<String> riskFactors, boolean requiresManualReview) {
        
        FraudAlert alert = FraudAlert.builder()
                .alertId(UUID.randomUUID().toString())
                .transactionId(transactionId)
                .userId(userId)
                .riskLevel(riskLevel)
                .riskScore(riskScore)
                .alertType("FRAUD_DETECTION")
                .message(String.format("Fraud alert: Risk level %s with score %d", riskLevel, riskScore))
                .riskFactors(riskFactors)
                .alertedAt(Instant.now())
                .requiresManualReview(requiresManualReview)
                .build();
        
        kafkaTemplate.send("fraud.alerts", transactionId, alert);
        log.info("Fraud alert sent for transaction {}: Risk level {}, Score {}", 
                transactionId, riskLevel, riskScore);
    }
}
