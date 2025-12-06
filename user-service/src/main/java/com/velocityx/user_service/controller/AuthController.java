package com.velocityx.user_service.controller;

import com.velocityx.user_service.dto.request.LoginRequest;
import com.velocityx.user_service.dto.request.RefreshTokenRequest;
import com.velocityx.user_service.dto.request.SignupRequest;
import com.velocityx.user_service.dto.response.JwtResponse;
import com.velocityx.user_service.security.UserPrincipal;
import com.velocityx.user_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication endpoints.
 * Implements production-grade API with proper versioning and documentation.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user", description = "Create a new user account")
    public ResponseEntity<JwtResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("Signup request received for email: {}", request.getEmail());
        JwtResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Login user", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("Login request received for email: {}", request.getEmail());
        JwtResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token", description = "Get new access token using refresh token")
    public ResponseEntity<JwtResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        log.info("Token refresh request received");
        JwtResponse response = authService.refreshToken(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Revoke all user tokens")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Logout request received for user: {}", userPrincipal.getEmail());
        authService.logout(userPrincipal.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/revoke-all")
    @Operation(summary = "Revoke all tokens", description = "Revoke all refresh tokens for the user")
    public ResponseEntity<Void> revokeAllTokens(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        log.info("Revoke all tokens request for user: {}", userPrincipal.getEmail());
        authService.revokeAllUserTokens(userPrincipal.getEmail());
        return ResponseEntity.noContent().build();
    }
}
