package com.velocityx.reward_service.exception;

import com.velocityx.reward_service.dto.response.ApiError;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {
    
    private final Tracer tracer;
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .toList();
        
        ApiError apiError = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Invalid request parameters")
                .path(request.getDescription(false).replace("uri=", ""))
                .traceId(getTraceId())
                .details(errors)
                .build();
        
        return ResponseEntity.badRequest().body(apiError);
    }
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("Runtime exception occurred", ex);
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        if (ex.getMessage() != null) {
            if (ex.getMessage().contains("not found")) {
                status = HttpStatus.NOT_FOUND;
            } else if (ex.getMessage().contains("Insufficient")) {
                status = HttpStatus.BAD_REQUEST;
            }
        }
        
        ApiError apiError = ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .traceId(getTraceId())
                .build();
        
        return ResponseEntity.status(status).body(apiError);
    }
    
    private String getTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return "N/A";
    }
}
