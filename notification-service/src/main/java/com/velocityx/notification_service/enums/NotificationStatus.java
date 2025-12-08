package com.velocityx.notification_service.enums;

/**
 * Notification delivery status
 */
public enum NotificationStatus {
    PENDING,           // Created, not yet processed
    PROCESSING,        // Being processed
    SUCCESS,           // Delivered successfully
    FAILED,            // Delivery failed
    RETRYING,          // In retry queue
    RATE_LIMITED,      // Rate limit exceeded, queued
    MAX_RETRIES_EXCEEDED,  // Exceeded max retry attempts
    DLQ                // Moved to dead letter queue
}
