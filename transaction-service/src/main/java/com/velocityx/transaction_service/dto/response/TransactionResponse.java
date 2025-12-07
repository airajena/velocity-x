package com.velocityx.transaction_service.dto.response;

import com.velocityx.transaction_service.enums.PaymentMethod;
import com.velocityx.transaction_service.enums.TransactionStatus;
import com.velocityx.transaction_service.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    
    private Long id;
    private String transactionId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    
    private Long userId;
    private Long fromWalletId;
    private Long toWalletId;
    
    private PaymentMethod paymentMethod;
    private String paymentGatewayRef;
    
    private String description;
    private Map<String, Object> metadata;
    private String failureReason;
    
    private Long parentTransactionId;
    
    private Instant holdExpiresAt;
    private Instant capturedAt;
    private Instant refundedAt;
    private Instant cancelledAt;
    
    private Instant processingStartedAt;
    private Instant completedAt;
    
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
