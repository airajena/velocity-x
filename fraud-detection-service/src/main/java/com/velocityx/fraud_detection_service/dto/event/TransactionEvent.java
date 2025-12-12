package com.velocityx.fraud_detection_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {
    
    private String transactionId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String status;
    
    // Context
    private String ipAddress;
    private String deviceId;
    private String userAgent;
    private String country;
    private String city;
    private Double latitude;
    private Double longitude;
    
    private Instant timestamp;
}
