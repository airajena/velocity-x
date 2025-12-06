package com.velocityx.user_service.entity;

import com.velocityx.user_service.security.Role;
import com.velocityx.user_service.util.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * User entity representing application users.
 * Implements production-grade security and audit features.
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_user_email", columnList = "email"),
        @Index(name = "idx_user_status", columnList = "account_status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.ROLE_USER;

    @Column(name = "account_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private java.time.Instant accountLockedUntil;

    @Column(name = "last_login_at")
    private java.time.Instant lastLoginAt;

    /**
     * Account status enum for user lifecycle management
     */
    public enum AccountStatus {
        ACTIVE,
        INACTIVE,
        LOCKED,
        SUSPENDED,
        PENDING_VERIFICATION
    }

    /**
     * Increment failed login attempts
     */
    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
    }

    /**
     * Reset failed login attempts
     */
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.accountLockedUntil = null;
    }

    /**
     * Lock account for specified duration
     */
    public void lockAccount(long durationInMinutes) {
        this.accountStatus = AccountStatus.LOCKED;
        this.accountLockedUntil = java.time.Instant.now().plusSeconds(durationInMinutes * 60);
    }

    /**
     * Check if account is locked
     */
    public boolean isAccountLocked() {
        if (accountStatus == AccountStatus.LOCKED && accountLockedUntil != null) {
            if (java.time.Instant.now().isAfter(accountLockedUntil)) {
                // Auto-unlock if lock period has expired
                this.accountStatus = AccountStatus.ACTIVE;
                this.accountLockedUntil = null;
                return false;
            }
            return true;
        }
        return false;
    }
}
