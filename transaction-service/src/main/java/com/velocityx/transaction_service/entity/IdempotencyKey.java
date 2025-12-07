package com.velocityx.transaction_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "idempotency_keys",
    indexes = {
        @Index(name = "idx_idempotency_key", columnList = "idempotency_key"),
        @Index(name = "idx_expires_at", columnList = "expires_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_idempotency_key", columnNames = "idempotency_key")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;
    
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "http_status")
    private Integer httpStatus;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
