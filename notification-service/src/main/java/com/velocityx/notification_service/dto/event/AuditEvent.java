package com.velocityx.notification_service.dto.event;

import com.velocityx.notification_service.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEvent {
    
    private String notificationId;
    private Long customerId;
    private Integer attemptNumber;
    private NotificationStatus status;
    private Integer responseCode;
    private String responseBody;
    private Long latencyMs;
    private String errorMessage;
    private Instant timestamp;
}
