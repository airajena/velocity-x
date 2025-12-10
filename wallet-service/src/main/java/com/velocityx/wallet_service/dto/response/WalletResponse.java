package com.velocityx.wallet_service.dto.response;

import com.velocityx.wallet_service.enums.WalletStatus;
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
public class WalletResponse {
    private Long id;
    private String walletId;
    private Long userId;
    private String currency;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private BigDecimal heldBalance;
    private WalletStatus status;
    private Instant createdAt;
}
