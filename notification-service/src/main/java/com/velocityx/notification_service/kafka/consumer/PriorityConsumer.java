package com.velocityx.notification_service.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocityx.notification_service.dto.event.NotificationEvent;
import com.velocityx.notification_service.entity.Notification;
import com.velocityx.notification_service.entity.NotificationAttempt;
import com.velocityx.notification_service.enums.NotificationStatus;
import com.velocityx.notification_service.repository.NotificationRepository;
import com.velocityx.notification_service.service.EmailDeliveryService;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PriorityConsumer {
    
    private final NotificationRepository notificationRepository;
    private final EmailDeliveryService emailDeliveryService;
    private final ObjectMapper objectMapper;
    private final Counter notificationSentCounter;
    private final Counter notificationFailedCounter;
    
    @KafkaListener(
            topics = {"${kafka.topics.notification-p0}", "${kafka.topics.notification-p1}", "${kafka.topics.notification-p2}"},
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeNotification(
            @Payload Map<String, Object> payload,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {
        
        log.info("Consuming notification: topic={}, offset={}, key={}", topic, offset, key);
        
        try {
            NotificationEvent event = objectMapper.convertValue(payload, NotificationEvent.class);
            
            Notification notification = notificationRepository.findByNotificationId(event.getNotificationId())
                    .orElseThrow(() -> new RuntimeException("Notification not found: " + event.getNotificationId()));
            
            processNotification(notification, event);
            
            acknowledgment.acknowledge();
            log.info("Notification processed successfully: {}", event.getNotificationId());
            
        } catch (Exception e) {
            log.error("Error processing notification: offset={}", offset, e);
            acknowledgment.acknowledge();
        }
    }
    
    private void processNotification(Notification notification, NotificationEvent event) {
        long startTime = System.currentTimeMillis();
        
        notification.setStatus(NotificationStatus.PROCESSING);
        notification.incrementAttempts();
        notificationRepository.save(notification);
        
        NotificationAttempt attempt = NotificationAttempt.builder()
                .notification(notification)
                .attemptNumber(notification.getCurrentAttempts())
                .status(NotificationStatus.PROCESSING)
                .attemptedAt(Instant.now())
                .build();
        
        try {
            String htmlContent = buildEmailContent(event);
            
            emailDeliveryService.sendEmail(
                    event.getRecipientEmail(),
                    event.getSubject(),
                    htmlContent,
                    null
            );
            
            long latency = System.currentTimeMillis() - startTime;
            
            attempt.setStatus(NotificationStatus.SUCCESS);
            attempt.setLatencyMs(latency);
            attempt.setResponseCode(200);
            
            notification.setStatus(NotificationStatus.SUCCESS);
            notification.setDeliveredAt(Instant.now());
            notification.addAttempt(attempt);
            
            notificationSentCounter.increment();
            
            log.info("Email delivered successfully: {} in {}ms", event.getNotificationId(), latency);
            
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            
            attempt.setStatus(NotificationStatus.FAILED);
            attempt.setLatencyMs(latency);
            attempt.setErrorMessage(e.getMessage());
            
            notification.setStatus(NotificationStatus.FAILED);
            notification.setLastError(e.getMessage());
            notification.addAttempt(attempt);
            
            notificationFailedCounter.increment();
            
            log.error("Email delivery failed: {}", event.getNotificationId(), e);
        }
        
        notificationRepository.save(notification);
    }
    
    private String buildEmailContent(NotificationEvent event) {
        if (event.getHtmlContent() != null && !event.getHtmlContent().isEmpty()) {
            return event.getHtmlContent();
        }
        
        if (event.getTemplateName() != null) {
            return emailDeliveryService.buildEmailFromTemplate(
                    event.getTemplateName(),
                    event.getTemplateData()
            );
        }
        
        return String.format("""
            <html>
            <body style="font-family: Arial, sans-serif;">
                <h2>Notification from VelocityX</h2>
                <p>Dear %s,</p>
                <p>You have a new notification.</p>
            </body>
            </html>
            """, event.getRecipientName() != null ? event.getRecipientName() : "Customer");
    }
}
