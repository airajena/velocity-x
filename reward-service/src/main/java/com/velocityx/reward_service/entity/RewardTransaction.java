package com.velocityx.reward_service.entity;

import com.velocityx.reward_service.enums.TransactionStatus;
import com.velocityx.reward_service.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(
    name = "reward_transactions",
    indexes = {
        @Index(name = "idx_txn_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_txn_user_id", columnList = "user_id"),
        @Index(name = "idx_txn_status", columnList = "status"),
        @Index(name = "idx_txn_type", columnList = "transaction_type"),
        @Index(name = "idx_txn_idempotency_key", columnList = "idempotency_key"),
        @Index(name = "idx_txn_hold_id", columnList = "hold_transaction_id"),
        @Index(name = "idx_txn_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_transaction_id", columnNames = "transaction_id"),
        @UniqueConstraint(name = "uk_idempotency_key", columnNames = "idempotency_key")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardTransaction extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id", nullable = false, unique = true, length = 50)
    private String transactionId;
    
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "account_id", nullable = false, length = 50)
    private String accountId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "hold_transaction_id", length = 50)
    private String holdTransactionId;
    
    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;
    
    @Column(name = "reference_transaction_id", length = 50)
    private String referenceTransactionId;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();
    
    public void addLedgerEntry(LedgerEntry entry) {
        ledgerEntries.add(entry);
        entry.setTransaction(this);
    }
    
    public void complete() {
        this.status = TransactionStatus.COMPLETED;
        this.completedAt = Instant.now();
    }
    
    public void fail(String errorMessage) {
        this.status = TransactionStatus.FAILED;
        this.errorMessage = errorMessage;
    }
    
    public boolean isHoldActive() {
        return status == TransactionStatus.HELD && 
               (holdExpiresAt == null || holdExpiresAt.isAfter(Instant.now()));
    }
}
