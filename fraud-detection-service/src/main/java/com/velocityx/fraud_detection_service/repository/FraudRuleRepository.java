package com.velocityx.fraud_detection_service.repository;

import com.velocityx.fraud_detection_service.entity.FraudRule;
import com.velocityx.fraud_detection_service.enums.FraudRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FraudRuleRepository extends JpaRepository<FraudRule, String> {
    
    Optional<FraudRule> findByRuleName(String ruleName);
    
    List<FraudRule> findByEnabledTrueOrderByPriorityDesc();
    
    List<FraudRule> findByRuleTypeAndEnabledTrue(FraudRuleType ruleType);
}
