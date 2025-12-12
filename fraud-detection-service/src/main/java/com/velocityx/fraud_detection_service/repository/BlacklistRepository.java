package com.velocityx.fraud_detection_service.repository;

import com.velocityx.fraud_detection_service.entity.BlacklistEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BlacklistRepository extends JpaRepository<BlacklistEntry, String> {
    
    Optional<BlacklistEntry> findByEntryTypeAndEntryValueAndActiveTrue(String entryType, String entryValue);
    
    List<BlacklistEntry> findByEntryTypeAndActiveTrue(String entryType);
    
    List<BlacklistEntry> findByActiveTrueAndExpiresAtBefore(Instant now);
}
