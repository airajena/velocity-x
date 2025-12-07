package com.velocityx.transaction_service.entity;

import com.velocityx.transaction_service.enums.EventType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(
    name = "transaction_events",
    indexes = {
        @Index(name = "idx_event_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_event_type", columnList = "event_type"),
        @Index(name = "idx_event_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, foreignKey = @ForeignKey(name = "fk_event_transaction"))
    private Transaction transaction;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_data", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> eventData = new HashMap<>();
    
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Column(name = "created_by", length = 255)
    private String createdBy;
}
