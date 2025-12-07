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
public class HoldFundsRequest {
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "1000000.00", message = "Amount cannot exceed 1,000,000")
    @Digits(integer = 15, fraction = 4)
    private BigDecimal amount;
    
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3)
    @Pattern(regexp = "^[A-Z]{3}$")
    private String currency = "USD";
    
    @NotNull(message = "User ID is required")
    @Positive
    private Long userId;
    
    @NotNull(message = "Wallet ID is required")
    @Positive
    private Long walletId;
    
    @Size(max = 1000)
    private String description;
    
    private Map<String, Object> metadata;
    
    @Size(max = 255)
    private String idempotencyKey;
    
    @Min(value = 1, message = "Hold duration must be at least 1 day")
    @Max(value = 30, message = "Hold duration cannot exceed 30 days")
    private Integer holdDurationDays = 7;
}
