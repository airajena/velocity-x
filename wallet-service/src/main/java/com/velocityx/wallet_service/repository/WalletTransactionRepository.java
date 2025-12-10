package com.velocityx.wallet_service.repository;

import com.velocityx.wallet_service.entity.WalletTransaction;
import com.velocityx.wallet_service.enums.TransactionStatus;
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
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    
    Optional<WalletTransaction> findByTransactionId(String transactionId);
    
    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    Page<WalletTransaction> findByUserId(Long userId, Pageable pageable);
    
    Page<WalletTransaction> findByWalletId(String walletId, Pageable pageable);
    
    @Query("SELECT t FROM WalletTransaction t WHERE t.status = :status AND t.holdExpiresAt < :now")
    List<WalletTransaction> findExpiredHolds(@Param("status") TransactionStatus status, @Param("now") Instant now);
    
    @Query("SELECT t FROM WalletTransaction t WHERE t.userId = :userId ORDER BY t.createdAt DESC")
    List<WalletTransaction> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);
}
