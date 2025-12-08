package com.velocityx.notification_service.repository;

import com.velocityx.notification_service.entity.NotificationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationAttemptRepository extends JpaRepository<NotificationAttempt, Long> {
    
    List<NotificationAttempt> findByNotificationIdOrderByAttemptNumberDesc(Long notificationId);
}
