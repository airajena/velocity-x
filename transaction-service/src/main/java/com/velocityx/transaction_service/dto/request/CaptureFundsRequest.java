package com.velocityx.transaction_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaptureFundsRequest {
    
    @Size(max = 1000)
    private String description;
    
    private Map<String, Object> metadata;
    
    @Size(max = 255)
    private String idempotencyKey;
}
