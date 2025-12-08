package com.velocityx.notification_service.service;

import com.velocityx.notification_service.dto.request.SendNotificationRequest;
import com.velocityx.notification_service.dto.response.NotificationResponse;
import com.velocityx.notification_service.dto.response.PagedResponse;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    
    NotificationResponse sendNotification(SendNotificationRequest request);
    
    NotificationResponse getNotificationById(Long id);
    
    NotificationResponse getNotificationByNotificationId(String notificationId);
    
    PagedResponse<NotificationResponse> getCustomerNotifications(Long customerId, Pageable pageable);
    
    PagedResponse<NotificationResponse> getUserNotifications(Long userId, Pageable pageable);
    
    void processRetries();
}
