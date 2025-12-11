package com.velocityx.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class RequestValidationFilter implements GlobalFilter, Ordered {
    
    private static final List<String> REQUIRED_HEADERS = List.of("Content-Type");
    private static final int MAX_REQUEST_SIZE = 10 * 1024 * 1024; // 10MB
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Skip validation for GET requests and public endpoints
        if (isPublicEndpoint(request.getPath().value()) || 
            "GET".equals(request.getMethod().name())) {
            return chain.filter(exchange);
        }
        
        // Validate Content-Type for POST/PUT requests
        if (("POST".equals(request.getMethod().name()) || "PUT".equals(request.getMethod().name()))) {
            String contentType = request.getHeaders().getFirst("Content-Type");
            if (contentType == null || contentType.isEmpty()) {
                log.warn("Missing Content-Type header for request: {}", request.getPath());
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().setComplete();
            }
        }
        
        // Validate request size
        Long contentLength = request.getHeaders().getContentLength();
        if (contentLength != null && contentLength > MAX_REQUEST_SIZE) {
            log.warn("Request size {} exceeds maximum allowed size {}", contentLength, MAX_REQUEST_SIZE);
            exchange.getResponse().setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
            return exchange.getResponse().setComplete();
        }
        
        return chain.filter(exchange);
    }
    
    private boolean isPublicEndpoint(String path) {
        return path.contains("/actuator") ||
               path.contains("/swagger-ui") ||
               path.contains("/api-docs") ||
               path.contains("/login") ||
               path.contains("/register");
    }
    
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
