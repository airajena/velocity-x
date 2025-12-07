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
public class WalletResponse {
    private Long id;
    private Long userId;
    private BigDecimal balance;
    private String currency;
    private String status;
}
