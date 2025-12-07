package com.velocityx.transaction_service.kafka.producer;

import com.velocityx.transaction_service.dto.event.NotificationEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventProducer {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topics.notification-events}")
    private String notificationEventsTopic;
    
    public void sendTransactionNotification(
            Long userId, 
            String email, 
            String transactionId, 
            String subject, 
            String message) {
        
        log.info("Sending transaction notification: userId={}, transactionId={}", userId, transactionId);
        
        NotificationEventDTO event = NotificationEventDTO.builder()
                .eventType("TRANSACTION_NOTIFICATION")
                .userId(userId)
                .email(email)
                .subject(subject)
                .message(message)
                .transactionId(transactionId)
                .channel("EMAIL")
                .timestamp(Instant.now())
                .build();
        
        kafkaTemplate.send(notificationEventsTopic, userId.toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Notification event sent successfully: userId={}", userId);
                    } else {
                        log.error("Failed to send notification event: userId={}", userId, ex);
                    }
                });
    }
    
    public void sendPaymentConfirmation(Long userId, String email, String transactionId, String amount) {
        String subject = "Payment Confirmation";
        String message = String.format(
                "Your payment of %s has been processed successfully. Transaction ID: %s", 
                amount, transactionId);
        
        sendTransactionNotification(userId, email, transactionId, subject, message);
    }
    
    public void sendRefundNotification(Long userId, String email, String transactionId, String amount) {
        String subject = "Refund Processed";
        String message = String.format(
                "Your refund of %s has been processed. Transaction ID: %s", 
                amount, transactionId);
        
        sendTransactionNotification(userId, email, transactionId, subject, message);
    }
    
    public void sendTransactionFailedNotification(Long userId, String email, String transactionId, String reason) {
        String subject = "Transaction Failed";
        String message = String.format(
                "Your transaction failed. Transaction ID: %s. Reason: %s", 
                transactionId, reason);
        
        sendTransactionNotification(userId, email, transactionId, subject, message);
    }
}
