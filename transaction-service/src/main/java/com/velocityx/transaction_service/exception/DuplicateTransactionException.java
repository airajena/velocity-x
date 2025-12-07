package com.velocityx.transaction_service.exception;

public class DuplicateTransactionException extends RuntimeException {
    public DuplicateTransactionException(String message) {
        super(message);
    }
    
    public DuplicateTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
