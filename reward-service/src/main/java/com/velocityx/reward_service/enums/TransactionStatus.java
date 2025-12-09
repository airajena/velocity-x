package com.velocityx.reward_service.enums;

public enum TransactionStatus {
    INIT,           // Initial state
    PENDING,        // Processing
    COMPLETED,      // Successfully completed
    FAILED,         // Failed
    HELD,           // Funds on hold
    CAPTURED,       // Hold captured
    RELEASED,       // Hold released
    EXPIRED,        // Hold expired
    CANCELLED       // Cancelled
}
