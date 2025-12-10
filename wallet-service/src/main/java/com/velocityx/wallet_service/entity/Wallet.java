package com.velocityx.wallet_service.entity;

import com.velocityx.wallet_service.enums.WalletStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
    name = "wallets",
    indexes = {
        @Index(name = "idx_wallet_user_id", columnList = "user_id"),
        @Index(name = "idx_wallet_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_wallet_currency", columnList = "currency")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallet_id", columnNames = "wallet_id"),
        @UniqueConstraint(name = "uk_user_currency", columnNames = {"user_id", "currency"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "wallet_id", nullable = false, unique = true, length = 50)
    private String walletId;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";
    
    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
    
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;
    
    @Column(name = "held_balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal heldBalance = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private WalletStatus status = WalletStatus.ACTIVE;
    
    public boolean hasAvailableBalance(BigDecimal amount) {
        return availableBalance.compareTo(amount) >= 0;
    }
    
    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }
    
    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
        this.availableBalance = this.availableBalance.subtract(amount);
    }
    
    public void holdFunds(BigDecimal amount) {
        this.availableBalance = this.availableBalance.subtract(amount);
        this.heldBalance = this.heldBalance.add(amount);
    }
    
    public void releaseHold(BigDecimal amount) {
        this.heldBalance = this.heldBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
    }
    
    public void captureHold(BigDecimal amount) {
        this.heldBalance = this.heldBalance.subtract(amount);
        this.balance = this.balance.subtract(amount);
    }
}
