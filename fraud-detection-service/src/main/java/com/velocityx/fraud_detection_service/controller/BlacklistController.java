package com.velocityx.fraud_detection_service.controller;

import com.velocityx.fraud_detection_service.entity.BlacklistEntry;
import com.velocityx.fraud_detection_service.repository.BlacklistRepository;
import com.velocityx.fraud_detection_service.service.BlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/blacklist")
@RequiredArgsConstructor
public class BlacklistController {
    
    private final BlacklistService blacklistService;
    private final BlacklistRepository blacklistRepository;
    
    @PostMapping
    public ResponseEntity<Void> addToBlacklist(
            @RequestParam String entryType,
            @RequestParam String entryValue,
            @RequestParam String reason,
            @RequestParam String addedBy,
            @RequestParam(required = false) Instant expiresAt) {
        
        blacklistService.addToBlacklist(entryType, entryValue, reason, addedBy, expiresAt);
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFromBlacklist(@PathVariable String id) {
        blacklistService.removeFromBlacklist(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping
    public ResponseEntity<List<BlacklistEntry>> getAllBlacklist() {
        return ResponseEntity.ok(blacklistRepository.findAll());
    }
    
    @PostMapping("/cleanup")
    public ResponseEntity<Void> cleanupExpired() {
        blacklistService.cleanupExpiredEntries();
        return ResponseEntity.ok().build();
    }
}
