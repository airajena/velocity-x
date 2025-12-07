package com.velocityx.transaction_service.repository;

import com.velocityx.transaction_service.entity.Transaction;
import com.velocityx.transaction_service.enums.TransactionStatus;
import com.velocityx.transaction_service.enums.TransactionType;
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
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Optional<Transaction> findByTransactionId(String transactionId);
    
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    Page<Transaction> findByUserId(Long userId, Pageable pageable);
    
    Page<Transaction> findByUserIdAndStatus(Long userId, TransactionStatus status, Pageable pageable);
    
    Page<Transaction> findByUserIdAndType(Long userId, TransactionType type, Pageable pageable);
    
    List<Transaction> findByFromWalletId(Long fromWalletId);
    
    List<Transaction> findByToWalletId(Long toWalletId);
    
    Page<Transaction> findByStatus(TransactionStatus status, Pageable pageable);
    
    Page<Transaction> findByType(TransactionType type, Pageable pageable);
    
    @Query("SELECT t FROM Transaction t WHERE t.userId = :userId AND t.createdAt BETWEEN :startDate AND :endDate")
    Page<Transaction> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable
    );
    
    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.holdExpiresAt < :now")
    List<Transaction> findExpiredHolds(@Param("status") TransactionStatus status, @Param("now") Instant now);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.userId = :userId AND t.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") TransactionStatus status);
    
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.userId = :userId AND t.type = :type AND t.status = :status")
    Double sumAmountByUserIdAndTypeAndStatus(
            @Param("userId") Long userId,
            @Param("type") TransactionType type,
            @Param("status") TransactionStatus status
    );
    
    @Query("SELECT t FROM Transaction t WHERE t.parentTransactionId = :parentId")
    List<Transaction> findByParentTransactionId(@Param("parentId") Long parentId);
}
