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
public class WalletEvent {
    
    private String walletId;
    private String userId;
    private String transactionId;
    private BigDecimal amount;
    private String currency;
    private String eventType;
    private String status;
    
    private String ipAddress;
    private String deviceId;
    
    private Instant timestamp;
}
