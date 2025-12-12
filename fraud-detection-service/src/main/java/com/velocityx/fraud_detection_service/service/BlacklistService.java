package com.velocityx.fraud_detection_service.service;

import com.velocityx.fraud_detection_service.dto.RiskScore;
import com.velocityx.fraud_detection_service.entity.BlacklistEntry;
import com.velocityx.fraud_detection_service.repository.BlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BlacklistService {
    
    private final BlacklistRepository blacklistRepository;
    
    public void checkBlacklist(String userId, String ipAddress, String deviceId, RiskScore riskScore) {
        log.debug("Checking blacklist for user: {}, IP: {}, device: {}", userId, ipAddress, deviceId);
        
        // Check user blacklist
        if (userId != null) {
            Optional<BlacklistEntry> userBlacklist = blacklistRepository
                    .findByEntryTypeAndEntryValueAndActiveTrue("USER_ID", userId);
            
            if (userBlacklist.isPresent()) {
                riskScore.addRiskFactor(
                        "BLACKLIST_USER",
                        "User is blacklisted: " + userBlacklist.get().getReason(),
                        100,
                        "CRITICAL"
                );
                log.error("Blacklisted user detected: {}", userId);
            }
        }
        
        // Check IP blacklist
        if (ipAddress != null) {
            Optional<BlacklistEntry> ipBlacklist = blacklistRepository
                    .findByEntryTypeAndEntryValueAndActiveTrue("IP_ADDRESS", ipAddress);
            
            if (ipBlacklist.isPresent()) {
                riskScore.addRiskFactor(
                        "BLACKLIST_IP",
                        "IP address is blacklisted: " + ipBlacklist.get().getReason(),
                        80,
                        "CRITICAL"
                );
                log.error("Blacklisted IP detected: {}", ipAddress);
            }
        }
        
        // Check device blacklist
        if (deviceId != null) {
            Optional<BlacklistEntry> deviceBlacklist = blacklistRepository
                    .findByEntryTypeAndEntryValueAndActiveTrue("DEVICE_ID", deviceId);
            
            if (deviceBlacklist.isPresent()) {
                riskScore.addRiskFactor(
                        "BLACKLIST_DEVICE",
                        "Device is blacklisted: " + deviceBlacklist.get().getReason(),
                        90,
                        "CRITICAL"
                );
                log.error("Blacklisted device detected: {}", deviceId);
            }
        }
    }
    
    public void addToBlacklist(String entryType, String entryValue, String reason, String addedBy, Instant expiresAt) {
        BlacklistEntry entry = BlacklistEntry.builder()
                .entryType(entryType)
                .entryValue(entryValue)
                .reason(reason)
                .active(true)
                .expiresAt(expiresAt)
                .addedBy(addedBy)
                .build();
        
        blacklistRepository.save(entry);
        log.info("Added to blacklist: {} = {}, reason: {}", entryType, entryValue, reason);
    }
    
    public void removeFromBlacklist(String id) {
        blacklistRepository.findById(id).ifPresent(entry -> {
            entry.setActive(false);
            blacklistRepository.save(entry);
            log.info("Removed from blacklist: {} = {}", entry.getEntryType(), entry.getEntryValue());
        });
    }
    
    public void cleanupExpiredEntries() {
        var expiredEntries = blacklistRepository.findByActiveTrueAndExpiresAtBefore(Instant.now());
        expiredEntries.forEach(entry -> {
            entry.setActive(false);
            blacklistRepository.save(entry);
        });
        log.info("Cleaned up {} expired blacklist entries", expiredEntries.size());
    }
}
