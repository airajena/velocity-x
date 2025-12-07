package com.velocityx.transaction_service.repository;

import com.velocityx.transaction_service.entity.TransactionEvent;
import com.velocityx.transaction_service.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface TransactionEventRepository extends JpaRepository<TransactionEvent, Long> {
    
    List<TransactionEvent> findByTransactionId(Long transactionId);
    
    List<TransactionEvent> findByEventType(EventType eventType);
    
    @Query("SELECT e FROM TransactionEvent e WHERE e.transaction.id = :transactionId ORDER BY e.createdAt ASC")
    List<TransactionEvent> findByTransactionIdOrderByCreatedAtAsc(@Param("transactionId") Long transactionId);
    
    @Query("SELECT e FROM TransactionEvent e WHERE e.createdAt BETWEEN :startDate AND :endDate")
    List<TransactionEvent> findByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate
    );
    
    @Query("SELECT COUNT(e) FROM TransactionEvent e WHERE e.eventType = :eventType")
    long countByEventType(@Param("eventType") EventType eventType);
}
