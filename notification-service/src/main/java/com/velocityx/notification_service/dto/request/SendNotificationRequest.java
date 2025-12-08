package com.velocityx.notification_service.dto.request;

import com.velocityx.notification_service.enums.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendNotificationRequest {
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    private Long userId;
    
    @NotNull(message = "Notification type is required")
    private NotificationType type;
    
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipientEmail;
    
    private String recipientName;
    
    @NotBlank(message = "Subject is required")
    private String subject;
    
    private String templateName;
    
    @Builder.Default
    private Map<String, Object> templateData = new HashMap<>();
    
    private String htmlContent;
    
    private String textContent;
}
