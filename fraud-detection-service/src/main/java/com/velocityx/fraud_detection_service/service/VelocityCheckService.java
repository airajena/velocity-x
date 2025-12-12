package com.velocityx.fraud_detection_service.service;

import com.velocityx.fraud_detection_service.dto.RiskScore;
import com.velocityx.fraud_detection_service.repository.FraudCheckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class VelocityCheckService {
    
    private final FraudCheckRepository fraudCheckRepository;
    
    @Value("${fraud.rules.velocity.max-transactions-per-hour:10}")
    private Integer maxTransactionsPerHour;
    
    @Value("${fraud.rules.velocity.max-transactions-per-day:50}")
    private Integer maxTransactionsPerDay;
    
    @Value("${fraud.rules.velocity.max-amount-per-hour:10000}")
    private BigDecimal maxAmountPerHour;
    
    @Value("${fraud.rules.velocity.max-amount-per-day:50000}")
    private BigDecimal maxAmountPerDay;
    
    public void checkVelocity(String userId, BigDecimal amount, RiskScore riskScore) {
        log.debug("Checking velocity for user: {}", userId);
        
        Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        
        // Check transaction count per hour
        Long transactionsLastHour = fraudCheckRepository.countByUserIdAndCreatedAtAfter(userId, oneHourAgo);
        if (transactionsLastHour >= maxTransactionsPerHour) {
            riskScore.addRiskFactor(
                    "VELOCITY_TRANSACTION_HOUR",
                    String.format("User has %d transactions in last hour (max: %d)", transactionsLastHour, maxTransactionsPerHour),
                    30,
                    "HIGH"
            );
            log.warn("High transaction velocity detected for user {}: {} transactions in last hour", userId, transactionsLastHour);
        }
        
        // Check transaction count per day
        Long transactionsLastDay = fraudCheckRepository.countByUserIdAndCreatedAtAfter(userId, oneDayAgo);
        if (transactionsLastDay >= maxTransactionsPerDay) {
            riskScore.addRiskFactor(
                    "VELOCITY_TRANSACTION_DAY",
                    String.format("User has %d transactions in last day (max: %d)", transactionsLastDay, maxTransactionsPerDay),
                    20,
                    "MEDIUM"
            );
            log.warn("High daily transaction velocity detected for user {}: {} transactions", userId, transactionsLastDay);
        }
        
        // Check amount per hour
        BigDecimal amountLastHour = fraudCheckRepository.sumAmountByUserIdAndCreatedAtAfter(userId, oneHourAgo);
        if (amountLastHour.compareTo(maxAmountPerHour) > 0) {
            riskScore.addRiskFactor(
                    "VELOCITY_AMOUNT_HOUR",
                    String.format("User has transacted %s in last hour (max: %s)", amountLastHour, maxAmountPerHour),
                    35,
                    "HIGH"
            );
            log.warn("High amount velocity detected for user {}: {} in last hour", userId, amountLastHour);
        }
        
        // Check amount per day
        BigDecimal amountLastDay = fraudCheckRepository.sumAmountByUserIdAndCreatedAtAfter(userId, oneDayAgo);
        if (amountLastDay.compareTo(maxAmountPerDay) > 0) {
            riskScore.addRiskFactor(
                    "VELOCITY_AMOUNT_DAY",
                    String.format("User has transacted %s in last day (max: %s)", amountLastDay, maxAmountPerDay),
                    25,
                    "MEDIUM"
            );
            log.warn("High daily amount velocity detected for user {}: {}", userId, amountLastDay);
        }
    }
}
