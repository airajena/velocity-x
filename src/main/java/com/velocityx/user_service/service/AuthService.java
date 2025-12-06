package com.velocityx.user_service.service;

import com.velocityx.user_service.dto.request.LoginRequest;
import com.velocityx.user_service.dto.request.RefreshTokenRequest;
import com.velocityx.user_service.dto.request.SignupRequest;
import com.velocityx.user_service.dto.response.JwtResponse;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Service interface for authentication operations.
 * Defines contract for signup, login, and token management.
 */
public interface AuthService {
    
    /**
     * Register a new user
     */
    JwtResponse signup(SignupRequest request);
    
    /**
     * Authenticate user and generate tokens
     */
    JwtResponse login(LoginRequest request, HttpServletRequest httpRequest);
    
    /**
     * Refresh access token using refresh token
     */
    JwtResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest);
    
    /**
     * Logout user and revoke tokens
     */
    void logout(String email);
    
    /**
     * Revoke all tokens for a user
     */
    void revokeAllUserTokens(String email);
}
