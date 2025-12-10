package com.velocityx.wallet_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest {
    
    @NotNull(message = "From user ID is required")
    private Long fromUserId;
    
    @NotNull(message = "To user ID is required")
    private Long toUserId;
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    @Builder.Default
    private String currency = "INR";
    
    private String description;
    
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
