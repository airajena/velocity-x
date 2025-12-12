package com.velocityx.fraud_detection_service.repository;

import com.velocityx.fraud_detection_service.entity.FraudCheck;
import com.velocityx.fraud_detection_service.enums.FraudCheckStatus;
import com.velocityx.fraud_detection_service.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface FraudCheckRepository extends JpaRepository<FraudCheck, String> {
    
    Optional<FraudCheck> findByTransactionId(String transactionId);
    
    List<FraudCheck> findByUserIdAndStatusOrderByCreatedAtDesc(String userId, FraudCheckStatus status);
    
    List<FraudCheck> findByRiskLevelOrderByCreatedAtDesc(RiskLevel riskLevel);
    
    @Query("SELECT COUNT(f) FROM FraudCheck f WHERE f.userId = :userId AND f.createdAt >= :since")
    Long countByUserIdAndCreatedAtAfter(@Param("userId") String userId, @Param("since") Instant since);
    
    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM FraudCheck f WHERE f.userId = :userId AND f.createdAt >= :since")
    BigDecimal sumAmountByUserIdAndCreatedAtAfter(@Param("userId") String userId, @Param("since") Instant since);
    
    @Query("SELECT f FROM FraudCheck f WHERE f.ipAddress = :ipAddress AND f.createdAt >= :since")
    List<FraudCheck> findByIpAddressAndCreatedAtAfter(@Param("ipAddress") String ipAddress, @Param("since") Instant since);
    
    @Query("SELECT f FROM FraudCheck f WHERE f.deviceId = :deviceId AND f.createdAt >= :since")
    List<FraudCheck> findByDeviceIdAndCreatedAtAfter(@Param("deviceId") String deviceId, @Param("since") Instant since);
    
    List<FraudCheck> findByStatusAndCreatedAtBetween(FraudCheckStatus status, Instant start, Instant end);
}
