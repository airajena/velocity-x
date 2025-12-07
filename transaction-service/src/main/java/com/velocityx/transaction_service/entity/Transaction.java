package com.velocityx.transaction_service.entity;

import com.velocityx.transaction_service.enums.PaymentMethod;
import com.velocityx.transaction_service.enums.TransactionStatus;
import com.velocityx.transaction_service.enums.TransactionType;
import com.velocityx.transaction_service.util.BaseEntity;
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
    name = "transactions",
    indexes = {
        @Index(name = "idx_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_type", columnList = "type"),
        @Index(name = "idx_created_at", columnList = "created_at"),
        @Index(name = "idx_idempotency_key", columnList = "idempotency_key"),
        @Index(name = "idx_from_wallet", columnList = "from_wallet_id"),
        @Index(name = "idx_to_wallet", columnList = "to_wallet_id")
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
public class Transaction extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "transaction_id", nullable = false, unique = true, length = 50)
    private String transactionId;
    
    @Column(name = "idempotency_key", unique = true, length = 255)
    private String idempotencyKey;
    
    // Transaction Details
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "USD";
    
    // Parties
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "from_wallet_id")
    private Long fromWalletId;
    
    @Column(name = "to_wallet_id")
    private Long toWalletId;
    
    // Payment Details
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 20)
    private PaymentMethod paymentMethod;
    
    @Column(name = "payment_gateway_ref", length = 255)
    private String paymentGatewayRef;
    
    // Metadata
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    
    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;
    
    // Related Transactions
    @Column(name = "parent_transaction_id")
    private Long parentTransactionId;
    
    // Hold/Capture specific
    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;
    
    @Column(name = "captured_at")
    private Instant capturedAt;
    
    @Column(name = "refunded_at")
    private Instant refundedAt;
    
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    
    // Processing timestamps
    @Column(name = "processing_started_at")
    private Instant processingStartedAt;
    
    @Column(name = "completed_at")
    private Instant completedAt;
    
    // Events
    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TransactionEvent> events = new ArrayList<>();
    
    // Helper methods
    public void addEvent(TransactionEvent event) {
        events.add(event);
        event.setTransaction(this);
    }
    
    public void removeEvent(TransactionEvent event) {
        events.remove(event);
        event.setTransaction(null);
    }
    
    public boolean isPending() {
        return status == TransactionStatus.PENDING;
    }
    
    public boolean isProcessing() {
        return status == TransactionStatus.PROCESSING;
    }
    
    public boolean isSuccess() {
        return status == TransactionStatus.SUCCESS;
    }
    
    public boolean isFailed() {
        return status == TransactionStatus.FAILED;
    }
    
    public boolean isHeld() {
        return status == TransactionStatus.HELD;
    }
    
    public boolean isCaptured() {
        return status == TransactionStatus.CAPTURED;
    }
    
    public boolean isCancelled() {
        return status == TransactionStatus.CANCELLED;
    }
    
    public boolean isRefunded() {
        return status == TransactionStatus.REFUNDED;
    }
    
    public boolean isCompleted() {
        return status == TransactionStatus.SUCCESS || 
               status == TransactionStatus.CAPTURED || 
               status == TransactionStatus.REFUNDED;
    }
    
    public boolean canBeRefunded() {
        return (status == TransactionStatus.SUCCESS || status == TransactionStatus.CAPTURED) 
               && type == TransactionType.DEBIT;
    }
    
    public boolean canBeCaptured() {
        return status == TransactionStatus.HELD && 
               (holdExpiresAt == null || holdExpiresAt.isAfter(Instant.now()));
    }
    
    public boolean canBeCancelled() {
        return status == TransactionStatus.PENDING || 
               status == TransactionStatus.HELD;
    }
}
