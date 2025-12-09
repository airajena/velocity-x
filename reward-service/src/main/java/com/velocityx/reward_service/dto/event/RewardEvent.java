package com.velocityx.reward_service.dto.event;

import com.velocityx.reward_service.enums.RewardEventType;
import com.velocityx.reward_service.enums.TransactionType;
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
public class RewardEvent {
    
    private String eventId;
    private RewardEventType eventType;
    private String transactionId;
    private String idempotencyKey;
    private Long userId;
    private String accountId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String description;
    private String holdTransactionId;
    private Map<String, Object> metadata;
    private String traceId;
    private Instant timestamp;
}
