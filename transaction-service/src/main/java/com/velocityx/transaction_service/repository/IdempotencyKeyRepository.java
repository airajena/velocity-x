package com.velocityx.transaction_service.repository;

import com.velocityx.transaction_service.entity.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {
    
    Optional<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    @Modifying
    @Query("DELETE FROM IdempotencyKey i WHERE i.expiresAt < :now")
    int deleteExpiredKeys(@Param("now") Instant now);
    
    @Query("SELECT COUNT(i) FROM IdempotencyKey i WHERE i.expiresAt > :now")
    long countActiveKeys(@Param("now") Instant now);
}
