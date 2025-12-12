package com.velocityx.fraud_detection_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "blacklist_entries", indexes = {
        @Index(name = "idx_entry_type", columnList = "entryType"),
        @Index(name = "idx_entry_value", columnList = "entryValue"),
        @Index(name = "idx_active", columnList = "active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlacklistEntry extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String entryType; // USER_ID, IP_ADDRESS, DEVICE_ID, EMAIL, CARD_NUMBER
    
    @Column(nullable = false)
    private String entryValue;
    
    @Column(length = 1000)
    private String reason;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    private Instant expiresAt;
    
    private String addedBy;
}
