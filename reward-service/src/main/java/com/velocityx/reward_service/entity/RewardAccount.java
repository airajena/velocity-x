package com.velocityx.reward_service.entity;

import com.velocityx.reward_service.enums.AccountType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "reward_accounts",
    indexes = {
        @Index(name = "idx_account_user_id", columnList = "user_id"),
        @Index(name = "idx_account_account_id", columnList = "account_id"),
        @Index(name = "idx_account_type", columnList = "account_type")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_id", columnNames = "account_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RewardAccount extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "account_id", nullable = false, unique = true, length = 50)
    private String accountId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;
    
    @Column(name = "balance_available", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balanceAvailable = BigDecimal.ZERO;
    
    @Column(name = "balance_reserved", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balanceReserved = BigDecimal.ZERO;
    
    @Column(name = "total_earned", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;
    
    @Column(name = "total_redeemed", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalRedeemed = BigDecimal.ZERO;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
    
    public BigDecimal getTotalBalance() {
        return balanceAvailable.add(balanceReserved);
    }
    
    public boolean hasAvailableBalance(BigDecimal amount) {
        return balanceAvailable.compareTo(amount) >= 0;
    }
    
    public void creditAvailable(BigDecimal amount) {
        this.balanceAvailable = this.balanceAvailable.add(amount);
        this.totalEarned = this.totalEarned.add(amount);
    }
    
    public void debitAvailable(BigDecimal amount) {
        this.balanceAvailable = this.balanceAvailable.subtract(amount);
    }
    
    public void holdFunds(BigDecimal amount) {
        this.balanceAvailable = this.balanceAvailable.subtract(amount);
        this.balanceReserved = this.balanceReserved.add(amount);
    }
    
    public void releaseHold(BigDecimal amount) {
        this.balanceReserved = this.balanceReserved.subtract(amount);
        this.balanceAvailable = this.balanceAvailable.add(amount);
    }
    
    public void captureHold(BigDecimal amount) {
        this.balanceReserved = this.balanceReserved.subtract(amount);
        this.totalRedeemed = this.totalRedeemed.add(amount);
    }
}
