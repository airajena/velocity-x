package com.velocityx.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocityx.notification_service.dto.request.SendNotificationRequest;
import com.velocityx.notification_service.enums.NotificationType;
import com.velocityx.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalEventConsumer {
    
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
            topics = {"${kafka.topics.transaction-events}"},
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransactionEvent(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received transaction event: {}", key);
            
            String eventType = (String) payload.get("eventType");
            Long userId = getLong(payload, "userId");
            String userEmail = (String) payload.get("userEmail");
            String userName = (String) payload.get("userName");
            
            if (userEmail == null || userEmail.isEmpty()) {
                log.warn("No email found in transaction event, skipping notification");
                acknowledgment.acknowledge();
                return;
            }
            
            SendNotificationRequest request = buildTransactionNotification(eventType, payload, userId, userEmail, userName);
            
            if (request != null) {
                notificationService.sendNotification(request);
                log.info("Transaction notification sent for event: {}", eventType);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing transaction event", e);
            acknowledgment.acknowledge();
        }
    }
    
    @KafkaListener(
            topics = {"${kafka.topics.user-events}"},
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserEvent(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            log.info("Received user event: {}", key);
            
            String eventType = (String) payload.get("eventType");
            Long userId = getLong(payload, "userId");
            String email = (String) payload.get("email");
            String name = (String) payload.get("name");
            
            if (email == null || email.isEmpty()) {
                log.warn("No email found in user event, skipping notification");
                acknowledgment.acknowledge();
                return;
            }
            
            SendNotificationRequest request = buildUserNotification(eventType, payload, userId, email, name);
            
            if (request != null) {
                notificationService.sendNotification(request);
                log.info("User notification sent for event: {}", eventType);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            log.error("Error processing user event", e);
            acknowledgment.acknowledge();
        }
    }
    
    private SendNotificationRequest buildTransactionNotification(
            String eventType, Map<String, Object> payload, Long userId, String email, String name) {
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("recipientName", name);
        templateData.put("transactionId", payload.get("transactionId"));
        templateData.put("amount", payload.get("amount"));
        templateData.put("date", payload.get("timestamp"));
        
        return switch (eventType) {
            case "TRANSACTION_SUCCESS", "TRANSACTION_COMPLETED" -> SendNotificationRequest.builder()
                    .customerId(1L)
                    .userId(userId)
                    .type(NotificationType.TRANSACTION_SUCCESS)
                    .recipientEmail(email)
                    .recipientName(name)
                    .subject("Transaction Successful")
                    .templateName("transaction_success")
                    .templateData(templateData)
                    .build();
                    
            case "TRANSACTION_FAILED" -> {
                templateData.put("reason", payload.get("errorMessage"));
                yield SendNotificationRequest.builder()
                        .customerId(1L)
                        .userId(userId)
                        .type(NotificationType.TRANSACTION_FAILED)
                        .recipientEmail(email)
                        .recipientName(name)
                        .subject("Transaction Failed")
                        .templateName("transaction_failed")
                        .templateData(templateData)
                        .build();
            }
                    
            case "REFUND_PROCESSED" -> SendNotificationRequest.builder()
                    .customerId(1L)
                    .userId(userId)
                    .type(NotificationType.REFUND_PROCESSED)
                    .recipientEmail(email)
                    .recipientName(name)
                    .subject("Refund Processed")
                    .templateName("transaction_success")
                    .templateData(templateData)
                    .build();
                    
            default -> null;
        };
    }
    
    private SendNotificationRequest buildUserNotification(
            String eventType, Map<String, Object> payload, Long userId, String email, String name) {
        
        Map<String, Object> templateData = new HashMap<>();
        templateData.put("recipientName", name);
        
        return switch (eventType) {
            case "USER_CREATED" -> SendNotificationRequest.builder()
                    .customerId(1L)
                    .userId(userId)
                    .type(NotificationType.ACCOUNT_CREATED)
                    .recipientEmail(email)
                    .recipientName(name)
                    .subject("Welcome to VelocityX!")
                    .templateName("account_created")
                    .templateData(templateData)
                    .build();
                    
            default -> null;
        };
    }
    
    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
}
