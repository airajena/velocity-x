package com.velocityx.reward_service.repository;

import com.velocityx.reward_service.entity.RewardTransaction;
import com.velocityx.reward_service.enums.TransactionStatus;
import com.velocityx.reward_service.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    
    Optional<RewardTransaction> findByTransactionId(String transactionId);
    
    Optional<RewardTransaction> findByIdempotencyKey(String idempotencyKey);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    Page<RewardTransaction> findByUserId(Long userId, Pageable pageable);
    
    Page<RewardTransaction> findByUserIdAndStatus(Long userId, TransactionStatus status, Pageable pageable);
    
    @Query("SELECT t FROM RewardTransaction t WHERE t.status = :status AND t.holdExpiresAt < :now")
    List<RewardTransaction> findExpiredHolds(
            @Param("status") TransactionStatus status,
            @Param("now") Instant now
    );
    
    @Query("SELECT t FROM RewardTransaction t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    List<RewardTransaction> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
    
    long countByUserIdAndTransactionType(Long userId, TransactionType transactionType);
}
