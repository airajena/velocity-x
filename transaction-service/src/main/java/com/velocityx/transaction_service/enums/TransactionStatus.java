package com.velocityx.transaction_service.enums;

/**
 * Transaction status lifecycle
 * 
 * Flow:
 * PENDING → PROCESSING → SUCCESS/FAILED
 * HELD → CAPTURED/CANCELLED
 * 
 * @author VelocityX Team
 */
public enum TransactionStatus {
    /**
     * Transaction created but not yet processed
     * Initial state for all transactions
     */
    PENDING,
    
    /**
     * Transaction is being processed
     * Wallet service is being called
     */
    PROCESSING,
    
    /**
     * Transaction completed successfully
     * Final state - money transferred
     */
    SUCCESS,
    
    /**
     * Transaction failed
     * Final state - money not transferred
     * Reason stored in failureReason field
     */
    FAILED,
    
    /**
     * Funds are held/reserved
     * Used for pre-authorization
     * Can be CAPTURED or CANCELLED
     */
    HELD,
    
    /**
     * Held funds have been captured
     * Final state - money transferred
     */
    CAPTURED,
    
    /**
     * Transaction was cancelled
     * Final state - money not transferred
     * Can be user-initiated or system-initiated
     */
    CANCELLED,
    
    /**
     * Transaction was refunded
     * Final state - money returned
     */
    REFUNDED,
    
    /**
     * Transaction was reversed
     * Final state - transaction undone
     */
    REVERSED
}
