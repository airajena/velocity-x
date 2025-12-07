package com.velocityx.transaction_service.enums;

/**
 * Event types for Kafka messaging
 * 
 * @author VelocityX Team
 */
public enum EventType {
    // Transaction Events
    TRANSACTION_CREATED,
    TRANSACTION_PROCESSING,
    TRANSACTION_SUCCESS,
    TRANSACTION_FAILED,
    TRANSACTION_HELD,
    TRANSACTION_CAPTURED,
    TRANSACTION_CANCELLED,
    TRANSACTION_REFUNDED,
    TRANSACTION_REVERSED,
    
    // Wallet Events (consumed from wallet-service)
    WALLET_CREDITED,
    WALLET_DEBITED,
    WALLET_HOLD_CREATED,
    WALLET_HOLD_RELEASED,
    WALLET_INSUFFICIENT_FUNDS,
    
    // User Events (consumed from user-service)
    USER_CREATED,
    USER_UPDATED,
    USER_DEACTIVATED,
    
    // Notification Events (produced for notification-service)
    SEND_TRANSACTION_NOTIFICATION,
    SEND_PAYMENT_CONFIRMATION,
    SEND_REFUND_NOTIFICATION
}
