package com.velocityx.fraud_detection_service.entity;

import com.velocityx.fraud_detection_service.enums.FraudCheckStatus;
import com.velocityx.fraud_detection_service.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fraud_checks", indexes = {
        @Index(name = "idx_transaction_id", columnList = "transactionId"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_risk_level", columnList = "riskLevel"),
        @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudCheck extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    private String transactionId;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    @Column(nullable = false)
    private String currency;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudCheckStatus status;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel;
    
    @Column(nullable = false)
    private Integer riskScore; // 0-100
    
    @Column(length = 2000)
    private String riskFactors; // JSON string of risk factors
    
    @Column(length = 1000)
    private String reason;
    
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
    
    // Velocity Metrics
    private Integer transactionsLastHour;
    private Integer transactionsLastDay;
    private BigDecimal amountLastHour;
    private BigDecimal amountLastDay;
    
    // Timestamps
    private Instant checkedAt;
    private Instant approvedAt;
    private Instant rejectedAt;
}
