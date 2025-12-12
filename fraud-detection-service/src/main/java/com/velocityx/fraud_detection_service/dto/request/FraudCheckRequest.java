package com.velocityx.fraud_detection_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCheckRequest {
    
    @NotBlank(message = "Transaction ID is required")
    private String transactionId;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    private String currency;
    
    // Transaction Details
    private String ipAddress;
    private String deviceId;
    private String deviceFingerprint;
    private String userAgent;
    
    // Geolocation
    private String country;
    private String city;
    private Double latitude;
    private Double longitude;
    
    // Additional Context
    private String merchantId;
    private String paymentMethod;
    private String transactionType;
}
