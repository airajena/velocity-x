package com.velocityx.notification_service.dto.event;

import com.velocityx.notification_service.enums.DeliveryChannel;
import com.velocityx.notification_service.enums.NotificationPriority;
import com.velocityx.notification_service.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {
    
    private String notificationId;
    private Long customerId;
    private Long userId;
    private NotificationPriority priority;
    private NotificationType type;
    private DeliveryChannel channel;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String templateName;
    
    @Builder.Default
    private Map<String, Object> templateData = new HashMap<>();
    
    private String htmlContent;
    private String textContent;
    private Instant timestamp;
    private String correlationId;
}
