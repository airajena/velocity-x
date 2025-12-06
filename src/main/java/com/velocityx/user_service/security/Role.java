package com.velocityx.user_service.security;

/**
 * Enum representing user roles in the system.
 * Follows Spring Security naming convention with ROLE_ prefix.
 */
public enum Role {
    ROLE_USER("User"),
    ROLE_ADMIN("Administrator"),
    ROLE_MODERATOR("Moderator");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAuthority() {
        return this.name();
    }
}
