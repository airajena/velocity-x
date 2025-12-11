package com.velocityx.api_gateway.util;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

@Component
public class RouteValidator {
    
    public static final List<String> OPEN_API_ENDPOINTS = List.of(
            "/api/users/register",
            "/api/users/login",
            "/actuator",
            "/swagger-ui",
            "/api-docs",
            "/gateway/health",
            "/gateway/info"
    );
    
    public Predicate<ServerHttpRequest> isSecured =
            request -> OPEN_API_ENDPOINTS.stream()
                    .noneMatch(uri -> request.getURI().getPath().contains(uri));
    
    public boolean isPublicEndpoint(String path) {
        return OPEN_API_ENDPOINTS.stream()
                .anyMatch(path::contains);
    }
    
    public boolean requiresAuthentication(ServerHttpRequest request) {
        return isSecured.test(request);
    }
}
