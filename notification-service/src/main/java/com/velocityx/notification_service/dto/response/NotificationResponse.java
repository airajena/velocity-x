package com.velocityx.notification_service.dto.response;

import com.velocityx.notification_service.enums.NotificationPriority;
import com.velocityx.notification_service.enums.NotificationStatus;
import com.velocityx.notification_service.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    
    private Long id;
    private String notificationId;
    private Long customerId;
    private Long userId;
    private NotificationPriority priority;
    private NotificationType type;
    private NotificationStatus status;
    private String recipientEmail;
    private String subject;
    private Integer currentAttempts;
    private Integer maxAttempts;
    private Instant nextRetryAt;
    private Instant deliveredAt;
    private Instant createdAt;
}
