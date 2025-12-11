package com.velocityx.eureka_server.controller;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/registry")
@RequiredArgsConstructor
@Slf4j
public class RegistryController {
    
    @GetMapping("/services")
    public ResponseEntity<Map<String, Object>> getAllServices() {
        EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
        PeerAwareInstanceRegistry registry = serverContext.getRegistry();
        
        List<Application> applications = registry.getSortedApplications();
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalServices", applications.size());
        response.put("services", applications.stream()
                .map(app -> {
                    Map<String, Object> serviceInfo = new HashMap<>();
                    serviceInfo.put("name", app.getName());
                    serviceInfo.put("instanceCount", app.getInstances().size());
                    serviceInfo.put("instances", app.getInstances().stream()
                            .map(instance -> {
                                Map<String, Object> instanceInfo = new HashMap<>();
                                instanceInfo.put("instanceId", instance.getInstanceId());
                                instanceInfo.put("hostName", instance.getHostName());
                                instanceInfo.put("ipAddr", instance.getIPAddr());
                                instanceInfo.put("port", instance.getPort());
                                instanceInfo.put("status", instance.getStatus().name());
                                return instanceInfo;
                            })
                            .collect(Collectors.toList()));
                    return serviceInfo;
                })
                .collect(Collectors.toList()));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/services/{serviceName}")
    public ResponseEntity<Map<String, Object>> getServiceDetails(@PathVariable String serviceName) {
        EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
        PeerAwareInstanceRegistry registry = serverContext.getRegistry();
        
        Application application = registry.getApplication(serviceName.toUpperCase());
        
        if (application == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("serviceName", application.getName());
        response.put("instanceCount", application.getInstances().size());
        response.put("instances", application.getInstances().stream()
                .map(instance -> {
                    Map<String, Object> instanceInfo = new HashMap<>();
                    instanceInfo.put("instanceId", instance.getInstanceId());
                    instanceInfo.put("hostName", instance.getHostName());
                    instanceInfo.put("ipAddr", instance.getIPAddr());
                    instanceInfo.put("port", instance.getPort());
                    instanceInfo.put("status", instance.getStatus().name());
                    instanceInfo.put("healthCheckUrl", instance.getHealthCheckUrl());
                    instanceInfo.put("statusPageUrl", instance.getStatusPageUrl());
                    return instanceInfo;
                })
                .collect(Collectors.toList()));
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "eureka-server");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
        PeerAwareInstanceRegistry registry = serverContext.getRegistry();
        
        List<Application> applications = registry.getSortedApplications();
        
        int totalInstances = applications.stream()
                .mapToInt(app -> app.getInstances().size())
                .sum();
        
        long upInstances = applications.stream()
                .flatMap(app -> app.getInstances().stream())
                .filter(instance -> instance.getStatus() == InstanceInfo.InstanceStatus.UP)
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalServices", applications.size());
        stats.put("totalInstances", totalInstances);
        stats.put("upInstances", upInstances);
        stats.put("downInstances", totalInstances - upInstances);
        
        return ResponseEntity.ok(stats);
    }
}
