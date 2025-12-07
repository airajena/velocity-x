package com.velocityx.transaction_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditRequest {
    private Long walletId;
    private BigDecimal amount;
    private String currency;
    private String transactionId;
    private String description;
}
