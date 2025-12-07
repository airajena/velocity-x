package com.velocityx.transaction_service.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefundRequest {
    
    @DecimalMin(value = "0.01", message = "Refund amount must be greater than 0")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;
    
    @NotBlank(message = "Refund reason is required")
    @Size(max = 1000, message = "Reason cannot exceed 1000 characters")
    private String reason;
    
    private Map<String, Object> metadata;
    
    @Size(max = 255)
    private String idempotencyKey;
}
