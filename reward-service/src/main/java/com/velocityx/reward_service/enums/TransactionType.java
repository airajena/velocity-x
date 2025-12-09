package com.velocityx.reward_service.enums;

public enum TransactionType {
    EARN,           // User earns points
    REDEEM,         // User redeems points
    HOLD,           // Reserve points for redemption
    CAPTURE,        // Convert hold to redemption
    RELEASE,        // Release hold
    REFUND,         // Refund points
    ADJUSTMENT,     // Admin adjustment
    EXPIRY          // Points expired
}
