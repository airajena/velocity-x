package com.velocityx.notification_service.repository;

import com.velocityx.notification_service.entity.Notification;
import com.velocityx.notification_service.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    Optional<Notification> findByNotificationId(String notificationId);
    
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    
    Page<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status, Pageable pageable);
    
    List<Notification> findByStatusAndNextRetryAtBefore(NotificationStatus status, Instant time);
    
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.currentAttempts < :maxRetries")
    List<Notification> findFailedNotificationsForRetry(
        @Param("status") NotificationStatus status,
        @Param("maxRetries") int maxRetries
    );
    
    Page<Notification> findByCustomerId(Long customerId, Pageable pageable);
    
    @Query("SELECT n FROM Notification n WHERE n.status = :status AND n.nextRetryAt < :time")
    List<Notification> findRetryableNotifications(
        @Param("status") NotificationStatus status,
        @Param("time") Instant time
    );
    
    long countByUserIdAndStatus(Long userId, NotificationStatus status);
}
