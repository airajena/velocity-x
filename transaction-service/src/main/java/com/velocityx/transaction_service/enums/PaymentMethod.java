package com.velocityx.transaction_service.enums;

/**
 * Payment methods supported by the system
 * 
 * @author VelocityX Team
 */
public enum PaymentMethod {
    /**
     * Wallet balance payment
     * Direct debit from user's wallet
     */
    WALLET,
    
    /**
     * Credit card payment
     * Processed through payment gateway
     */
    CREDIT_CARD,
    
    /**
     * Debit card payment
     * Processed through payment gateway
     */
    DEBIT_CARD,
    
    /**
     * Net banking payment
     * Bank transfer
     */
    NET_BANKING,
    
    /**
     * UPI payment (India)
     * Unified Payments Interface
     */
    UPI,
    
    /**
     * Bank transfer
     * Direct bank-to-bank transfer
     */
    BANK_TRANSFER,
    
    /**
     * Cash payment
     * Physical cash
     */
    CASH,
    
    /**
     * Other payment methods
     * Fallback for new payment methods
     */
    OTHER
}
