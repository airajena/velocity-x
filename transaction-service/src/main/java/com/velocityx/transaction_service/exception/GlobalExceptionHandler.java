package com.velocityx.transaction_service.exception;

import com.velocityx.transaction_service.dto.response.ApiError;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    
    private final Tracer tracer;
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());
        
        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }
    
    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiError> handleInsufficientFunds(
            InsufficientFundsException ex,
            HttpServletRequest request) {
        
        log.warn("Insufficient funds: {}", ex.getMessage());
        
        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.PAYMENT_REQUIRED.value())
                .error("Insufficient Funds")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(error);
    }
    
    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<ApiError> handleDuplicateTransaction(
            DuplicateTransactionException ex,
            HttpServletRequest request) {
        
        log.warn("Duplicate transaction: {}", ex.getMessage());
        
        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Duplicate Transaction")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
    
    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ApiError> handleInvalidTransaction(
            InvalidTransactionException ex,
            HttpServletRequest request) {
        
        log.warn("Invalid transaction: {}", ex.getMessage());
        
        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Invalid Transaction")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(TransactionProcessingException.class)
    public ResponseEntity<ApiError> handleTransactionProcessing(
            TransactionProcessingException ex,
            HttpServletRequest request) {
        
        log.error("Transaction processing error: {}", ex.getMessage(), ex);
        
        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Transaction Processing Error")
                .message("An error occurred while processing the transaction")
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        log.warn("Validation failed");
        
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        
        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid input")
                .path(request.getRequestURI())
                .validationErrors(fieldErrors)
                .traceId(getTraceId())
                .build();
        
        return ResponseEntity.badRequest().body(error);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Unexpected error", ex);
        
        ApiError error = ApiError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("An unexpected error occurred")
                .path(request.getRequestURI())
                .traceId(getTraceId())
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    private String getTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return null;
    }
}
