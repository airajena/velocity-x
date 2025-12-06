package com.velocityx.user_service.service;

import com.velocityx.user_service.dto.response.PagedResponse;
import com.velocityx.user_service.dto.response.UserResponse;
import com.velocityx.user_service.entity.User;
import com.velocityx.user_service.exception.ResourceNotFoundException;
import com.velocityx.user_service.mapper.UserMapper;
import com.velocityx.user_service.repository.UserRepository;
import com.velocityx.user_service.security.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Production-grade user service implementation.
 * Implements user management with pagination and search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user by ID: {}", id);
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Fetching all users with pagination: {}", pageable);
        
        Page<User> userPage = userRepository.findAll(pageable);
        
        return buildPagedResponse(userPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> searchUsers(String searchTerm, Pageable pageable) {
        log.debug("Searching users with term: {}", searchTerm);
        
        Page<User> userPage = userRepository.searchUsers(searchTerm, pageable);
        
        return buildPagedResponse(userPage);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getUsersByRole(Role role, Pageable pageable) {
        log.debug("Fetching users by role: {}", role);
        
        Page<User> userPage = userRepository.findByRole(role, pageable);
        
        return buildPagedResponse(userPage);
    }

    @Override
    @Transactional
    public UserResponse updateUserRole(Long userId, Role newRole) {
        log.info("Updating role for user ID: {} to {}", userId, newRole);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        user.setRole(newRole);
        user = userRepository.save(user);
        
        log.info("Role updated successfully for user: {}", user.getEmail());
        
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    public void deactivateUser(Long userId) {
        log.info("Deactivating user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        user.setAccountStatus(User.AccountStatus.INACTIVE);
        userRepository.save(user);
        
        log.info("User deactivated: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void activateUser(Long userId) {
        log.info("Activating user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        user.setAccountStatus(User.AccountStatus.ACTIVE);
        userRepository.save(user);
        
        log.info("User activated: {}", user.getEmail());
    }

    /**
     * Build paged response from Page object
     */
    private PagedResponse<UserResponse> buildPagedResponse(Page<User> userPage) {
        List<UserResponse> content = userPage.getContent()
                .stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());

        return PagedResponse.<UserResponse>builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .last(userPage.isLast())
                .first(userPage.isFirst())
                .build();
    }
}
