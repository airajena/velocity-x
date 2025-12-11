package com.velocityx.api_gateway.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gateway")
@RequiredArgsConstructor
@Slf4j
public class GatewayController {
    
    private final RouteDefinitionLocator routeDefinitionLocator;
    
    @GetMapping("/routes")
    public Mono<ResponseEntity<List<RouteDefinition>>> getRoutes() {
        return routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .map(ResponseEntity::ok);
    }
    
    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, Object>>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "api-gateway");
        health.put("timestamp", System.currentTimeMillis());
        
        return Mono.just(ResponseEntity.ok(health));
    }
    
    @GetMapping("/info")
    public Mono<ResponseEntity<Map<String, Object>>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "VelocityX API Gateway");
        info.put("version", "1.0.0");
        info.put("description", "Production-grade API Gateway");
        
        return routeDefinitionLocator.getRouteDefinitions()
                .count()
                .map(count -> {
                    info.put("totalRoutes", count);
                    return ResponseEntity.ok(info);
                });
    }
}
