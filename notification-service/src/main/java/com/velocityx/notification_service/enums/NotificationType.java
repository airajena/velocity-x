package com.velocityx.notification_service.enums;

/**
 * Notification types for priority classification
 */
public enum NotificationType {
    // P0 - Critical
    TRANSACTION_SUCCESS,
    TRANSACTION_FAILED,
    PAYMENT_RECEIVED,
    PAYMENT_FAILED,
    REFUND_PROCESSED,
    
    // P1 - Default
    ACCOUNT_CREATED,
    PASSWORD_RESET,
    EMAIL_VERIFICATION,
    PROFILE_UPDATED,
    WALLET_CREDITED,
    WALLET_DEBITED,
    
    // P2 - Bulk
    MARKETING,
    NEWSLETTER,
    PROMOTIONAL,
    ANNOUNCEMENT
}
