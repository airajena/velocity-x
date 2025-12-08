package com.velocityx.notification_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "customer_qos",
    indexes = {
        @Index(name = "idx_qos_customer_id", columnList = "customer_id"),
        @Index(name = "idx_qos_is_throttled", columnList = "is_throttled")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_customer_id", columnNames = "customer_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerQoS {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "customer_id", nullable = false, unique = true)
    private Long customerId;
    
    @Column(name = "avg_response_time_ms")
    @Builder.Default
    private Long avgResponseTimeMs = 0L;
    
    @Column(name = "slow_request_count")
    @Builder.Default
    private Long slowRequestCount = 0L;
    
    @Column(name = "total_request_count")
    @Builder.Default
    private Long totalRequestCount = 0L;
    
    @Column(name = "concurrent_limit")
    @Builder.Default
    private Integer concurrentLimit = 5;
    
    @Column(name = "is_throttled")
    @Builder.Default
    private Boolean isThrottled = false;
    
    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();
    
    public void updateMetrics(long responseTimeMs, boolean isSlow) {
        this.totalRequestCount++;
        if (isSlow) {
            this.slowRequestCount++;
        }
        
        long totalTime = this.avgResponseTimeMs * (this.totalRequestCount - 1) + responseTimeMs;
        this.avgResponseTimeMs = totalTime / this.totalRequestCount;
        this.updatedAt = Instant.now();
    }
}
