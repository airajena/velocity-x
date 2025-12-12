package com.velocityx.fraud_detection_service.service;

import com.velocityx.fraud_detection_service.dto.RiskScore;
import com.velocityx.fraud_detection_service.repository.FraudCheckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceFingerprintService {
    
    private final FraudCheckRepository fraudCheckRepository;
    
    @Value("${fraud.rules.device.new-device-risk-score:30}")
    private Integer newDeviceRiskScore;
    
    @Value("${fraud.rules.device.suspicious-device-risk-score:50}")
    private Integer suspiciousDeviceRiskScore;
    
    public void checkDeviceFingerprint(String userId, String deviceId, String deviceFingerprint, RiskScore riskScore) {
        if (deviceId == null && deviceFingerprint == null) {
            riskScore.addRiskFactor(
                    "DEVICE_NO_FINGERPRINT",
                    "No device information provided",
                    15,
                    "MEDIUM"
            );
            log.warn("No device information provided for user: {}", userId);
            return;
        }
        
        log.debug("Checking device fingerprint for user: {}, device: {}", userId, deviceId);
        
        // Check if device is new
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        var deviceHistory = deviceId != null 
                ? fraudCheckRepository.findByDeviceIdAndCreatedAtAfter(deviceId, thirtyDaysAgo)
                : null;
        
        if (deviceHistory == null || deviceHistory.isEmpty()) {
            riskScore.addRiskFactor(
                    "DEVICE_NEW",
                    "Transaction from new or unknown device",
                    newDeviceRiskScore,
                    "MEDIUM"
            );
            log.info("New device detected for user: {}", userId);
        }
        
        // Check for suspicious device patterns
        if (deviceHistory != null && deviceHistory.size() > 1) {
            long uniqueUsers = deviceHistory.stream()
                    .map(check -> check.getUserId())
                    .distinct()
                    .count();
            
            if (uniqueUsers > 5) {
                riskScore.addRiskFactor(
                        "DEVICE_MULTIPLE_USERS",
                        String.format("Device used by %d different users", uniqueUsers),
                        suspiciousDeviceRiskScore,
                        "HIGH"
                );
                log.warn("Suspicious device detected: used by {} users", uniqueUsers);
            }
        }
    }
}
