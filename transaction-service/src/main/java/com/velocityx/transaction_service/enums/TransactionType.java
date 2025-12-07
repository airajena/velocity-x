package com.velocityx.transaction_service.enums;

/**
 * Transaction types supported by the system
 * 
 * @author VelocityX Team
 */
public enum TransactionType {
    /**
     * Credit transaction - Money added to wallet
     * Examples: Deposit, Refund, Cashback
     */
    CREDIT,
    
    /**
     * Debit transaction - Money deducted from wallet
     * Examples: Payment, Withdrawal, Transfer (sender)
     */
    DEBIT,
    
    /**
     * Transfer transaction - Money moved between wallets
     * Creates two transactions: DEBIT (sender) and CREDIT (receiver)
     */
    TRANSFER,
    
    /**
     * Hold transaction - Money reserved but not deducted
     * Used for pre-authorization (like PayPal)
     */
    HOLD,
    
    /**
     * Capture transaction - Convert held funds to actual debit
     * Completes a HOLD transaction
     */
    CAPTURE,
    
    /**
     * Refund transaction - Return money to original payer
     * Reverses a previous DEBIT transaction
     */
    REFUND,
    
    /**
     * Reversal transaction - Cancel a transaction
     * Used for failed/cancelled transactions
     */
    REVERSAL
}
