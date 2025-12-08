package com.velocityx.notification_service.service;

import com.velocityx.notification_service.dto.event.NotificationEvent;
import com.velocityx.notification_service.dto.request.SendNotificationRequest;
import com.velocityx.notification_service.dto.response.NotificationResponse;
import com.velocityx.notification_service.dto.response.PagedResponse;
import com.velocityx.notification_service.entity.Notification;
import com.velocityx.notification_service.enums.DeliveryChannel;
import com.velocityx.notification_service.enums.NotificationPriority;
import com.velocityx.notification_service.enums.NotificationStatus;
import com.velocityx.notification_service.repository.NotificationRepository;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final RedisRateLimiter rateLimiter;
    private final EmailDeliveryService emailDeliveryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Counter notificationSentCounter;
    private final Counter notificationFailedCounter;
    private final Counter rateLimitHitCounter;
    
    @Override
    public NotificationResponse sendNotification(SendNotificationRequest request) {
        log.info("Sending notification: type={}, to={}", request.getType(), request.getRecipientEmail());
        
        NotificationPriority priority = NotificationPriority.fromType(request.getType());
        
        if (!rateLimiter.isAllowed(request.getCustomerId(), priority.name())) {
            rateLimitHitCounter.increment();
            log.warn("Rate limit exceeded for customer: {}", request.getCustomerId());
            return handleRateLimitedNotification(request, priority);
        }
        
        Notification notification = createNotification(request, priority);
        notification = notificationRepository.save(notification);
        
        publishToKafka(notification, priority);
        
        return toResponse(notification);
    }
    
    private Notification createNotification(SendNotificationRequest request, NotificationPriority priority) {
        return Notification.builder()
                .notificationId(generateNotificationId())
                .customerId(request.getCustomerId())
                .userId(request.getUserId())
                .priority(priority)
                .type(request.getType())
                .channel(DeliveryChannel.EMAIL)
                .status(NotificationStatus.PENDING)
                .recipientEmail(request.getRecipientEmail())
                .recipientName(request.getRecipientName())
                .subject(request.getSubject())
                .templateName(request.getTemplateName())
                .payload(request.getTemplateData())
                .build();
    }
    
    private void publishToKafka(Notification notification, NotificationPriority priority) {
        NotificationEvent event = NotificationEvent.builder()
                .notificationId(notification.getNotificationId())
                .customerId(notification.getCustomerId())
                .userId(notification.getUserId())
                .priority(priority)
                .type(notification.getType())
                .channel(notification.getChannel())
                .recipientEmail(notification.getRecipientEmail())
                .recipientName(notification.getRecipientName())
                .subject(notification.getSubject())
                .templateName(notification.getTemplateName())
                .templateData(notification.getPayload())
                .timestamp(Instant.now())
                .build();
        
        String topic = priority.getKafkaTopic();
        kafkaTemplate.send(topic, notification.getNotificationId(), event);
        
        log.info("Published notification to Kafka: topic={}, id={}", topic, notification.getNotificationId());
    }
    
    private NotificationResponse handleRateLimitedNotification(SendNotificationRequest request, NotificationPriority priority) {
        Notification notification = createNotification(request, priority);
        notification.setStatus(NotificationStatus.RATE_LIMITED);
        notification = notificationRepository.save(notification);
        
        kafkaTemplate.send("notification-rate-limit", notification.getNotificationId(), 
                toNotificationEvent(notification));
        
        return toResponse(notification);
    }
    
    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));
        return toResponse(notification);
    }
    
    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationByNotificationId(String notificationId) {
        Notification notification = notificationRepository.findByNotificationId(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        return toResponse(notification);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getCustomerNotifications(Long customerId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByCustomerId(customerId, pageable);
        return toPagedResponse(page);
    }
    
    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getUserNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByUserId(userId, pageable);
        return toPagedResponse(page);
    }
    
    @Override
    @Scheduled(fixedDelay = 60000)
    public void processRetries() {
        log.debug("Processing retries...");
        
        List<Notification> retryable = notificationRepository.findRetryableNotifications(
                NotificationStatus.RETRYING, Instant.now());
        
        for (Notification notification : retryable) {
            if (notification.canRetry()) {
                log.info("Retrying notification: {}", notification.getNotificationId());
                publishToKafka(notification, notification.getPriority());
            } else {
                notification.setStatus(NotificationStatus.MAX_RETRIES_EXCEEDED);
                notificationRepository.save(notification);
                kafkaTemplate.send("notification-dlq", notification.getNotificationId(), 
                        toNotificationEvent(notification));
            }
        }
    }
    
    private String generateNotificationId() {
        return "NOTIF-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 16);
    }
    
    private NotificationEvent toNotificationEvent(Notification notification) {
        return NotificationEvent.builder()
                .notificationId(notification.getNotificationId())
                .customerId(notification.getCustomerId())
                .userId(notification.getUserId())
                .priority(notification.getPriority())
                .type(notification.getType())
                .channel(notification.getChannel())
                .recipientEmail(notification.getRecipientEmail())
                .recipientName(notification.getRecipientName())
                .subject(notification.getSubject())
                .templateName(notification.getTemplateName())
                .templateData(notification.getPayload())
                .timestamp(Instant.now())
                .build();
    }
    
    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .notificationId(notification.getNotificationId())
                .customerId(notification.getCustomerId())
                .userId(notification.getUserId())
                .priority(notification.getPriority())
                .type(notification.getType())
                .status(notification.getStatus())
                .recipientEmail(notification.getRecipientEmail())
                .subject(notification.getSubject())
                .currentAttempts(notification.getCurrentAttempts())
                .maxAttempts(notification.getMaxAttempts())
                .nextRetryAt(notification.getNextRetryAt())
                .deliveredAt(notification.getDeliveredAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
    
    private PagedResponse<NotificationResponse> toPagedResponse(Page<Notification> page) {
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        
        return PagedResponse.<NotificationResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .empty(page.isEmpty())
                .build();
    }
}
