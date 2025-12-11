package com.velocityx.api_gateway.filter;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TraceIdFilter implements GlobalFilter, Ordered {
    
    private final Tracer tracer;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = getOrCreateTraceId();
        
        // Add trace ID to request headers
        exchange.getRequest().mutate()
                .header("X-Trace-Id", traceId)
                .build();
        
        // Add trace ID to response headers
        exchange.getResponse().getHeaders().add("X-Trace-Id", traceId);
        
        log.debug("Request processed with Trace ID: {}", traceId);
        
        return chain.filter(exchange);
    }
    
    private String getOrCreateTraceId() {
        if (tracer != null && tracer.currentSpan() != null) {
            return tracer.currentSpan().context().traceId();
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
