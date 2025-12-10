package com.velocityx.wallet_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaptureRequest {
    
    @NotBlank(message = "Hold transaction ID is required")
    private String holdTransactionId;
    
    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;
}
