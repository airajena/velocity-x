package com.velocityx.transaction_service.client.dto;

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
public class HoldResponse {
    private String holdReference;
    private Long walletId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private Instant expiresAt;
}
