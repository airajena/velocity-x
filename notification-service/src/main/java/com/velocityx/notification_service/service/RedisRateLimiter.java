package com.velocityx.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisRateLimiter {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    @Value("${notification.rate-limit.window-seconds:60}")
    private int windowSeconds;
    
    @Value("${notification.rate-limit.p0-limit:1000}")
    private int p0Limit;
    
    @Value("${notification.rate-limit.p1-limit:500}")
    private int p1Limit;
    
    @Value("${notification.rate-limit.p2-limit:100}")
    private int p2Limit;
    
    public boolean isAllowed(Long customerId, String priority) {
        String key = String.format("ratelimit:%s:%s", customerId, priority);
        
        Long count = redisTemplate.opsForValue().increment(key);
        
        if (count == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
        }
        
        int limit = getLimit(priority);
        boolean allowed = count <= limit;
        
        if (!allowed) {
            log.warn("Rate limit exceeded for customer: {}, priority: {}, count: {}", 
                    customerId, priority, count);
        }
        
        return allowed;
    }
    
    private int getLimit(String priority) {
        return switch (priority) {
            case "P0" -> p0Limit;
            case "P1" -> p1Limit;
            case "P2" -> p2Limit;
            default -> p1Limit;
        };
    }
    
    public long getRemainingQuota(Long customerId, String priority) {
        String key = String.format("ratelimit:%s:%s", customerId, priority);
        Long count = (Long) redisTemplate.opsForValue().get(key);
        
        if (count == null) {
            return getLimit(priority);
        }
        
        return Math.max(0, getLimit(priority) - count);
    }
}
