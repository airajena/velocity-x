package com.velocityx.transaction_service.dto.event;

import com.velocityx.transaction_service.enums.EventType;
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
public class TransactionEventDTO {
    
    private String transactionId;
    private EventType eventType;
    private String type;
    private String status;
    private BigDecimal amount;
    private String currency;
    private Long userId;
    private Long fromWalletId;
    private Long toWalletId;
    private Map<String, Object> metadata;
    private Instant timestamp;
    private String correlationId;
}
