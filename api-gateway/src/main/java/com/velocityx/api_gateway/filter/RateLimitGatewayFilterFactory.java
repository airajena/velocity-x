package com.velocityx.api_gateway.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitGatewayFilterFactory extends AbstractGatewayFilterFactory<RateLimitGatewayFilterFactory.Config> {
    
    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final ConcurrentHashMap<String, Bucket> cache = new ConcurrentHashMap<>();
    
    public RateLimitGatewayFilterFactory() {
        super(Config.class);
    }
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            String key = userId != null ? userId : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            
            Bucket bucket = cache.computeIfAbsent(key, k -> createNewBucket(config));
            
            if (bucket.tryConsume(1)) {
                log.debug("Request allowed for key: {}", key);
                return chain.filter(exchange);
            } else {
                log.warn("Rate limit exceeded for key: {}", key);
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().add("X-Rate-Limit-Retry-After-Seconds", "60");
                return exchange.getResponse().setComplete();
            }
        };
    }
    
    private Bucket createNewBucket(Config config) {
        Bandwidth limit = Bandwidth.classic(
                config.getCapacity(),
                Refill.intervally(config.getRefillTokens(), Duration.ofSeconds(config.getRefillPeriod()))
        );
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
    
    public static class Config {
        private int capacity = 100;
        private int refillTokens = 100;
        private int refillPeriod = 60;
        
        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
        
        public int getRefillTokens() { return refillTokens; }
        public void setRefillTokens(int refillTokens) { this.refillTokens = refillTokens; }
        
        public int getRefillPeriod() { return refillPeriod; }
        public void setRefillPeriod(int refillPeriod) { this.refillPeriod = refillPeriod; }
    }
}
