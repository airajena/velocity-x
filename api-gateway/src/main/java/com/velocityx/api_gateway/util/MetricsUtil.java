package com.velocityx.api_gateway.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MetricsUtil {
    
    private final MeterRegistry meterRegistry;
    
    public void incrementRequestCounter(String service, String method, String status) {
        Counter.builder("gateway.requests.total")
                .tag("service", service)
                .tag("method", method)
                .tag("status", status)
                .description("Total number of requests through gateway")
                .register(meterRegistry)
                .increment();
    }
    
    public void recordRequestDuration(String service, long durationMs) {
        Timer.builder("gateway.request.duration")
                .tag("service", service)
                .description("Request duration in milliseconds")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void incrementRateLimitCounter(String userId) {
        Counter.builder("gateway.ratelimit.exceeded")
                .tag("user", userId)
                .description("Number of rate limit exceeded events")
                .register(meterRegistry)
                .increment();
    }
    
    public void incrementCircuitBreakerCounter(String service, String state) {
        Counter.builder("gateway.circuitbreaker.state")
                .tag("service", service)
                .tag("state", state)
                .description("Circuit breaker state changes")
                .register(meterRegistry)
                .increment();
    }
    
    public void incrementAuthFailureCounter(String reason) {
        Counter.builder("gateway.auth.failures")
                .tag("reason", reason)
                .description("Authentication failures")
                .register(meterRegistry)
                .increment();
    }
}
