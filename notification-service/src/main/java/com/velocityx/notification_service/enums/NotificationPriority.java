package com.velocityx.notification_service.enums;

import lombok.Getter;

/**
 * Notification priority levels following Razorpay architecture
 * P0 - Critical: High-impact, transactional events (1000/min, 10 threads)
 * P1 - Default: Standard, non-critical events (500/min, 5 threads)
 * P2 - Bulk: High-volume, low-priority events (100/min, 3 threads)
 */
@Getter
public enum NotificationPriority {
    
    P0("CRITICAL", 1000, 10, "notification-p0"),
    P1("DEFAULT", 500, 5, "notification-p1"),
    P2("BULK", 100, 3, "notification-p2");
    
    private final String displayName;
    private final int maxRatePerMinute;
    private final int consumerThreads;
    private final String kafkaTopic;
    
    NotificationPriority(String displayName, int maxRatePerMinute, int consumerThreads, String kafkaTopic) {
        this.displayName = displayName;
        this.maxRatePerMinute = maxRatePerMinute;
        this.consumerThreads = consumerThreads;
        this.kafkaTopic = kafkaTopic;
    }
    
    public static NotificationPriority fromType(NotificationType type) {
        return switch (type) {
            case TRANSACTION_SUCCESS, TRANSACTION_FAILED, PAYMENT_RECEIVED -> P0;
            case ACCOUNT_CREATED, PASSWORD_RESET, EMAIL_VERIFICATION -> P1;
            case MARKETING, NEWSLETTER, PROMOTIONAL -> P2;
        };
    }
}
