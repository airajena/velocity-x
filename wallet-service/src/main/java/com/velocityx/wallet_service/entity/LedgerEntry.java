package com.velocityx.wallet_service.entity;

import com.velocityx.wallet_service.enums.LedgerEntryType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "ledger_entries",
    indexes = {
        @Index(name = "idx_ledger_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_ledger_wallet_id", columnList = "wallet_id"),
        @Index(name = "idx_ledger_entry_type", columnList = "entry_type"),
        @Index(name = "idx_ledger_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_ledger_entry", columnNames = {"transaction_id", "entry_side", "wallet_id"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "entry_id", nullable = false, unique = true, length = 50)
    private String entryId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private WalletTransaction transaction;
    
    @Column(name = "wallet_id", nullable = false, length = 50)
    private String walletId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private LedgerEntryType entryType;
    
    @Column(name = "entry_side", nullable = false, length = 10)
    private String entrySide;
    
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;
    
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;
    
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
