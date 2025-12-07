package com.velocityx.transaction_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaptureRequest {
    private String holdReference;
    private String transactionId;
    private String description;
}
