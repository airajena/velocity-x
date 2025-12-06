package com.velocityx.user_service.exception;

/**
 * Exception thrown when a user account is locked.
 */
public class AccountLockedException extends RuntimeException {
    
    private final java.time.Instant lockedUntil;
    
    public AccountLockedException(String message, java.time.Instant lockedUntil) {
        super(message);
        this.lockedUntil = lockedUntil;
    }
    
    public java.time.Instant getLockedUntil() {
        return lockedUntil;
    }
}
