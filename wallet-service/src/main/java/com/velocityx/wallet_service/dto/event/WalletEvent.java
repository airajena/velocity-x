package com.velocityx.wallet_service.dto.event;

import com.velocityx.wallet_service.enums.TransactionType;
import com.velocityx.wallet_service.enums.WalletEventType;
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
public class WalletEvent {
    private String eventId;
    private WalletEventType eventType;
    private String transactionId;
    private String idempotencyKey;
    private Long userId;
    private String walletId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String holdTransactionId;
    private String counterpartyWalletId;
    private Long counterpartyUserId;
    private Map<String, Object> metadata;
    private String traceId;
    private Instant timestamp;
}
