package com.velocityx.reward_service.dto.response;

import com.velocityx.reward_service.enums.TransactionStatus;
import com.velocityx.reward_service.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    
    private Long id;
    private String transactionId;
    private Long userId;
    private TransactionType transactionType;
    private TransactionStatus status;
    private BigDecimal amount;
    private String description;
    private String holdTransactionId;
    private Instant holdExpiresAt;
    private Instant completedAt;
    private Instant createdAt;
}
