package com.velocityx.notification_service.dto.event;

import com.velocityx.notification_service.enums.NotificationPriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetryEvent {
    
    private String notificationId;
    private NotificationPriority priority;
    private Integer attemptNumber;
    private Instant nextRetryAt;
    private String lastError;
    private Instant timestamp;
}
