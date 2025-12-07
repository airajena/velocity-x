package com.velocityx.transaction_service.dto.event;

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
public class WalletEventDTO {
    
    private String eventType;
    private Long walletId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
    private String transactionId;
    private String status;
    private String reason;
    private Instant timestamp;
}
