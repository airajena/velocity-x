package com.velocityx.transaction_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {
    
    private final ObjectMapper objectMapper;
    
    @KafkaListener(
            topics = "${kafka.topics.user-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeUserEvent(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {
        
        log.info("Received user event: topic={}, offset={}, key={}", topic, offset, key);
        
        try {
            String eventType = (String) payload.get("eventType");
            Long userId = ((Number) payload.get("userId")).longValue();
            
            processUserEvent(eventType, userId, payload);
            
            acknowledgment.acknowledge();
            log.info("User event processed successfully: eventType={}, userId={}", eventType, userId);
            
        } catch (Exception e) {
            log.error("Error processing user event: offset={}", offset, e);
            acknowledgment.acknowledge();
        }
    }
    
    private void processUserEvent(String eventType, Long userId, Map<String, Object> payload) {
        switch (eventType) {
            case "USER_CREATED":
                handleUserCreated(userId, payload);
                break;
            case "USER_UPDATED":
                handleUserUpdated(userId, payload);
                break;
            case "USER_DEACTIVATED":
                handleUserDeactivated(userId, payload);
                break;
            default:
                log.warn("Unknown user event type: {}", eventType);
        }
    }
    
    private void handleUserCreated(Long userId, Map<String, Object> payload) {
        log.info("Processing user created event: userId={}", userId);
        // Future: Initialize user-specific transaction settings
    }
    
    private void handleUserUpdated(Long userId, Map<String, Object> payload) {
        log.info("Processing user updated event: userId={}", userId);
        // Future: Update user-related transaction data if needed
    }
    
    private void handleUserDeactivated(Long userId, Map<String, Object> payload) {
        log.info("Processing user deactivated event: userId={}", userId);
        // Future: Handle pending transactions for deactivated user
    }
}
