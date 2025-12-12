package com.velocityx.fraud_detection_service.entity;

import com.velocityx.fraud_detection_service.enums.FraudRuleType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "fraud_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudRule extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false, unique = true)
    private String ruleName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FraudRuleType ruleType;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private Boolean enabled = true;
    
    @Column(nullable = false)
    private Integer priority; // Higher number = higher priority
    
    @Column(nullable = false)
    private Integer riskScoreImpact; // How much this rule adds to risk score
    
    // Rule Parameters (stored as JSON or individual fields)
    private BigDecimal thresholdAmount;
    private Integer thresholdCount;
    private Integer timeWindowMinutes;
    private Double geoDistanceKm;
    
    @Column(length = 2000)
    private String ruleConfig; // JSON for complex rules
}
