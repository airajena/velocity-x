package com.velocityx.notification_service.controller;

import com.velocityx.notification_service.dto.request.SendNotificationRequest;
import com.velocityx.notification_service.dto.response.NotificationResponse;
import com.velocityx.notification_service.dto.response.PagedResponse;
import com.velocityx.notification_service.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification Management", description = "APIs for managing notifications")
@SecurityRequirement(name = "bearer-jwt")
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @PostMapping
    @Operation(summary = "Send notification", description = "Send a new notification")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        log.info("REST request to send notification: {}", request);
        NotificationResponse response = notificationService.sendNotification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get notification by ID", description = "Retrieve notification details by ID")
    public ResponseEntity<NotificationResponse> getNotificationById(@PathVariable Long id) {
        log.info("REST request to get notification: {}", id);
        NotificationResponse response = notificationService.getNotificationById(id);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/notif/{notificationId}")
    @Operation(summary = "Get notification by notification ID", description = "Retrieve notification by notification ID")
    public ResponseEntity<NotificationResponse> getNotificationByNotificationId(
            @PathVariable String notificationId) {
        log.info("REST request to get notification by notificationId: {}", notificationId);
        NotificationResponse response = notificationService.getNotificationByNotificationId(notificationId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get customer notifications", description = "Retrieve all notifications for a customer")
    public ResponseEntity<PagedResponse<NotificationResponse>> getCustomerNotifications(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("REST request to get notifications for customer: {}", customerId);
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PagedResponse<NotificationResponse> response = 
                notificationService.getCustomerNotifications(customerId, pageable);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user notifications", description = "Retrieve all notifications for a user")
    public ResponseEntity<PagedResponse<NotificationResponse>> getUserNotifications(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        log.info("REST request to get notifications for user: {}", userId);
        
        Sort sort = sortDir.equalsIgnoreCase("ASC") ? 
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        PagedResponse<NotificationResponse> response = 
                notificationService.getUserNotifications(userId, pageable);
        return ResponseEntity.ok(response);
    }
}
