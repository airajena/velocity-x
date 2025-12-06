package com.velocityx.user_service.repository;

import com.velocityx.user_service.entity.RefreshToken;
import com.velocityx.user_service.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for RefreshToken entity.
 * Implements token management with revocation and cleanup.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true, rt.revokedAt = :revokedAt WHERE rt.user = :user")
    void revokeAllUserTokens(@Param("user") User user, @Param("revokedAt") Instant revokedAt);
    
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now OR rt.revoked = true")
    void deleteExpiredAndRevokedTokens(@Param("now") Instant now);
    
    long countByUserAndRevokedFalse(User user);
}
