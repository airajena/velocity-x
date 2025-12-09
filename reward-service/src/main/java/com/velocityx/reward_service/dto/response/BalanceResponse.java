package com.velocityx.reward_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {
    
    private Long userId;
    private String accountId;
    private BigDecimal balanceAvailable;
    private BigDecimal balanceReserved;
    private BigDecimal totalBalance;
    private BigDecimal totalEarned;
    private BigDecimal totalRedeemed;
    private List<TransactionResponse> recentTransactions;
}
