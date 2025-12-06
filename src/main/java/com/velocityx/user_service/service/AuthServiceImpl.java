package com.velocityx.user_service.service;

import com.velocityx.user_service.dto.request.LoginRequest;
import com.velocityx.user_service.dto.request.RefreshTokenRequest;
import com.velocityx.user_service.dto.request.SignupRequest;
import com.velocityx.user_service.dto.response.JwtResponse;
import com.velocityx.user_service.dto.response.UserResponse;
import com.velocityx.user_service.entity.RefreshToken;
import com.velocityx.user_service.entity.User;
import com.velocityx.user_service.exception.AccountLockedException;
import com.velocityx.user_service.exception.AuthenticationException;
import com.velocityx.user_service.exception.ResourceNotFoundException;
import com.velocityx.user_service.mapper.UserMapper;
import com.velocityx.user_service.repository.RefreshTokenRepository;
import com.velocityx.user_service.repository.UserRepository;
import com.velocityx.user_service.security.JwtProvider;
import com.velocityx.user_service.security.Role;
import com.velocityx.user_service.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Production-grade authentication service implementation.
 * Implements secure signup, login, token refresh, and account locking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final UserMapper userMapper;

    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-duration-minutes:30}")
    private long lockDurationMinutes;

    @Value("${app.security.admin-key:CHANGE_THIS_ADMIN_SECRET_KEY}")
    private String adminSecretKey;

    @Override
    @Transactional
    public JwtResponse signup(SignupRequest request) {
        log.info("Processing signup request for email: {}", request.getEmail());

        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Create new user
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .accountStatus(User.AccountStatus.ACTIVE)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .build();

        // Check for admin key
        if (request.getAdminKey() != null && adminSecretKey.equals(request.getAdminKey())) {
            user.setRole(Role.ROLE_ADMIN);
            log.info("Admin account created for email: {}", request.getEmail());
        }

        user = userRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        // Generate tokens
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = jwtProvider.generateAccessToken(userPrincipal);
        String refreshToken = createRefreshToken(user, null, null);

        UserResponse userResponse = userMapper.toUserResponse(user);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public JwtResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Processing login request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        // Check if account is locked
        if (user.isAccountLocked()) {
            throw new AccountLockedException(
                    "Account is locked due to multiple failed login attempts. Please try again later.",
                    user.getAccountLockedUntil()
            );
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedLogin(user);
            throw new AuthenticationException("Invalid email or password");
        }

        // Check account status
        if (user.getAccountStatus() != User.AccountStatus.ACTIVE) {
            throw new AuthenticationException("Account is not active. Status: " + user.getAccountStatus());
        }

        // Successful login - reset failed attempts
        user.resetFailedAttempts();
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        log.info("User logged in successfully: {}", user.getEmail());

        // Generate tokens
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String accessToken = jwtProvider.generateAccessToken(userPrincipal);
        
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String refreshToken = createRefreshToken(user, ipAddress, userAgent);

        UserResponse userResponse = userMapper.toUserResponse(user);

        return JwtResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public JwtResponse refreshToken(RefreshTokenRequest request, HttpServletRequest httpRequest) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            throw new AuthenticationException("Refresh token is expired or revoked");
        }

        User user = refreshToken.getUser();
        
        // Generate new access token
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        String newAccessToken = jwtProvider.generateAccessToken(userPrincipal);

        // Optionally rotate refresh token (best practice)
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);

        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        String newRefreshToken = createRefreshToken(user, ipAddress, userAgent);

        UserResponse userResponse = userMapper.toUserResponse(user);

        log.info("Tokens refreshed for user: {}", user.getEmail());

        return JwtResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProvider.getAccessTokenExpirationSeconds())
                .user(userResponse)
                .build();
    }

    @Override
    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        refreshTokenRepository.revokeAllUserTokens(user, Instant.now());
        log.info("User logged out: {}", email);
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        refreshTokenRepository.revokeAllUserTokens(user, Instant.now());
        log.info("All tokens revoked for user: {}", email);
    }

    /**
     * Handle failed login attempt
     */
    private void handleFailedLogin(User user) {
        user.incrementFailedAttempts();

        if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
            user.lockAccount(lockDurationMinutes);
            log.warn("Account locked for user: {} due to {} failed attempts", 
                    user.getEmail(), maxFailedAttempts);
        }

        userRepository.save(user);
    }

    /**
     * Create and persist refresh token
     */
    private String createRefreshToken(User user, String ipAddress, String userAgent) {
        String tokenString = jwtProvider.generateRefreshToken(user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .user(user)
                .expiresAt(jwtProvider.getRefreshTokenExpiryInstant())
                .revoked(false)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenString;
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}
