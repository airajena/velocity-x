package com.velocityx.notification_service.entity;

import com.velocityx.notification_service.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "notification_attempts",
    indexes = {
        @Index(name = "idx_attempt_notification_id", columnList = "notification_id"),
        @Index(name = "idx_attempt_status", columnList = "status"),
        @Index(name = "idx_attempted_at", columnList = "attempted_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationAttempt {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attempt_notification"))
    private Notification notification;
    
    @Column(name = "attempt_number", nullable = false)
    private Integer attemptNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;
    
    @Column(name = "response_code")
    private Integer responseCode;
    
    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;
    
    @Column(name = "latency_ms")
    private Long latencyMs;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "attempted_at", nullable = false)
    @Builder.Default
    private Instant attemptedAt = Instant.now();
}
