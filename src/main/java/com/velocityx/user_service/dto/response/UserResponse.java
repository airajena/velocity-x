package com.velocityx.user_service.dto.response;

import com.velocityx.user_service.security.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for user response.
 * Never exposes sensitive information like passwords.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private Role role;
    private String accountStatus;
    private Boolean emailVerified;
    private Instant lastLoginAt;
    private Instant createdAt;
}
