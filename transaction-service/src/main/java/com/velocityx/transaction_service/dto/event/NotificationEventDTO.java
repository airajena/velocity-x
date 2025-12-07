package com.velocityx.transaction_service.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEventDTO {
    
    private String eventType;
    private Long userId;
    private String email;
    private String subject;
    private String message;
    private String transactionId;
    private String channel; // EMAIL, SMS, PUSH
    private Instant timestamp;
}
