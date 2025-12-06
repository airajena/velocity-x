package com.velocityx.user_service.security;

import com.velocityx.user_service.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Custom UserDetails implementation for Spring Security.
 * Wraps User entity with Spring Security requirements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long id;
    private String name;
    private String email;
    private String password;
    private Role role;
    private User.AccountStatus accountStatus;

    public static UserPrincipal create(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getRole(),
                user.getAccountStatus()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority(role.getAuthority())
        );
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountStatus != User.AccountStatus.LOCKED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return accountStatus == User.AccountStatus.ACTIVE;
    }
}
