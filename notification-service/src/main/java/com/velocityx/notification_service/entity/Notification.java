package com.velocityx.notification_service.entity;

import com.velocityx.notification_service.enums.DeliveryChannel;
import com.velocityx.notification_service.enums.NotificationPriority;
import com.velocityx.notification_service.enums.NotificationStatus;
import com.velocityx.notification_service.enums.NotificationType;
import com.velocityx.notification_service.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Table(
    name = "notifications",
    indexes = {
        @Index(name = "idx_notification_id", columnList = "notification_id"),
        @Index(name = "idx_customer_id", columnList = "customer_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_priority", columnList = "priority"),
        @Index(name = "idx_next_retry_at", columnList = "next_retry_at"),
        @Index(name = "idx_created_at", columnList = "created_at")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_id", columnNames = "notification_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "notification_id", nullable = false, unique = true, length = 50)
    private String notificationId;
    
    @Column(name = "customer_id", nullable = false)
    private Long customerId;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 10)
    private NotificationPriority priority;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private DeliveryChannel channel;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NotificationStatus status;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();
    
    @Column(name = "recipient_email", length = 255)
    private String recipientEmail;
    
    @Column(name = "recipient_name", length = 255)
    private String recipientName;
    
    @Column(name = "subject", length = 500)
    private String subject;
    
    @Column(name = "template_name", length = 100)
    private String templateName;
    
    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 5;
    
    @Column(name = "current_attempts", nullable = false)
    @Builder.Default
    private Integer currentAttempts = 0;
    
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
    
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    
    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<NotificationAttempt> attempts = new ArrayList<>();
    
    public void addAttempt(NotificationAttempt attempt) {
        attempts.add(attempt);
        attempt.setNotification(this);
    }
    
    public void incrementAttempts() {
        this.currentAttempts++;
    }
    
    public boolean canRetry() {
        return currentAttempts < maxAttempts;
    }
    
    public boolean isMaxRetriesExceeded() {
        return currentAttempts >= maxAttempts;
    }
}
