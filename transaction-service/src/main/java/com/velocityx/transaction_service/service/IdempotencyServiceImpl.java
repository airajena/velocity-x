package com.velocityx.transaction_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.velocityx.transaction_service.dto.response.TransactionResponse;
import com.velocityx.transaction_service.entity.IdempotencyKey;
import com.velocityx.transaction_service.repository.IdempotencyKeyRepository;
import io.micrometer.core.instrument.Counter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class IdempotencyServiceImpl implements IdempotencyService {
    
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;
    private final Counter idempotencyHitCounter;
    
    @Value("${app.transaction.idempotency.ttl-hours:24}")
    private int ttlHours;
    
    @Override
    @Transactional(readOnly = true)
    public <T> TransactionResponse getCachedResponse(String idempotencyKey, T request) {
        log.debug("Checking idempotency key: {}", idempotencyKey);
        
        Optional<IdempotencyKey> cached = idempotencyKeyRepository.findByIdempotencyKey(idempotencyKey);
        
        if (cached.isEmpty()) {
            return null;
        }
        
        IdempotencyKey key = cached.get();
        
        if (key.isExpired()) {
            log.debug("Idempotency key expired: {}", idempotencyKey);
            idempotencyKeyRepository.delete(key);
            return null;
        }
        
        String requestHash = hashRequest(request);
        if (!requestHash.equals(key.getRequestHash())) {
            log.warn("Idempotency key conflict: same key, different request");
            return null;
        }
        
        try {
            idempotencyHitCounter.increment();
            log.info("Idempotency key hit: {}", idempotencyKey);
            return objectMapper.readValue(key.getResponseBody(), TransactionResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize cached response", e);
            return null;
        }
    }
    
    @Override
    public <T> void cacheResponse(String idempotencyKey, T request, TransactionResponse response) {
        log.debug("Caching response for idempotency key: {}", idempotencyKey);
        
        try {
            String requestHash = hashRequest(request);
            String responseBody = objectMapper.writeValueAsString(response);
            
            IdempotencyKey key = IdempotencyKey.builder()
                    .idempotencyKey(idempotencyKey)
                    .requestHash(requestHash)
                    .responseBody(responseBody)
                    .httpStatus(200)
                    .expiresAt(Instant.now().plus(ttlHours, ChronoUnit.HOURS))
                    .build();
            
            idempotencyKeyRepository.save(key);
            
            log.info("Cached response for idempotency key: {}", idempotencyKey);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize response for caching", e);
        }
    }
    
    @Override
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void cleanupExpiredKeys() {
        log.info("Starting cleanup of expired idempotency keys");
        
        int deleted = idempotencyKeyRepository.deleteExpiredKeys(Instant.now());
        
        log.info("Cleaned up {} expired idempotency keys", deleted);
    }
    
    private <T> String hashRequest(T request) {
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(requestJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            log.error("Failed to hash request", e);
            return "";
        }
    }
}
