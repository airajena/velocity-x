package com.velocityx.user_service.service;

import com.velocityx.user_service.dto.response.PagedResponse;
import com.velocityx.user_service.dto.response.UserResponse;
import com.velocityx.user_service.security.Role;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for user management operations.
 */
public interface UserService {
    
    /**
     * Get user by ID
     */
    UserResponse getUserById(Long id);
    
    /**
     * Get user by email
     */
    UserResponse getUserByEmail(String email);
    
    /**
     * Get all users with pagination
     */
    PagedResponse<UserResponse> getAllUsers(Pageable pageable);
    
    /**
     * Search users by name or email
     */
    PagedResponse<UserResponse> searchUsers(String searchTerm, Pageable pageable);
    
    /**
     * Get users by role
     */
    PagedResponse<UserResponse> getUsersByRole(Role role, Pageable pageable);
    
    /**
     * Update user role (admin only)
     */
    UserResponse updateUserRole(Long userId, Role newRole);
    
    /**
     * Deactivate user account
     */
    void deactivateUser(Long userId);
    
    /**
     * Activate user account
     */
    void activateUser(Long userId);
}
