package com.velocityx.notification_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    
    @Bean
    public Counter notificationSentCounter(MeterRegistry registry) {
        return Counter.builder("notification.sent")
                .description("Total notifications sent")
                .tag("service", "notification-service")
                .register(registry);
    }
    
    @Bean
    public Counter notificationFailedCounter(MeterRegistry registry) {
        return Counter.builder("notification.failed")
                .description("Total notifications failed")
                .tag("service", "notification-service")
                .register(registry);
    }
    
    @Bean
    public Counter rateLimitHitCounter(MeterRegistry registry) {
        return Counter.builder("ratelimit.hit")
                .description("Rate limit hits")
                .tag("service", "notification-service")
                .register(registry);
    }
    
    @Bean
    public Timer emailDeliveryTimer(MeterRegistry registry) {
        return Timer.builder("email.delivery.duration")
                .description("Email delivery duration")
                .tag("service", "notification-service")
                .register(registry);
    }
}
