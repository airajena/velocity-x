package com.velocityx.transaction_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    
    @Bean
    public Counter transactionCreatedCounter(MeterRegistry registry) {
        return Counter.builder("transaction.created")
                .description("Total number of transactions created")
                .tag("service", "transaction-service")
                .register(registry);
    }
    
    @Bean
    public Counter transactionSuccessCounter(MeterRegistry registry) {
        return Counter.builder("transaction.success")
                .description("Total number of successful transactions")
                .tag("service", "transaction-service")
                .register(registry);
    }
    
    @Bean
    public Counter transactionFailedCounter(MeterRegistry registry) {
        return Counter.builder("transaction.failed")
                .description("Total number of failed transactions")
                .tag("service", "transaction-service")
                .register(registry);
    }
    
    @Bean
    public Timer transactionProcessingTimer(MeterRegistry registry) {
        return Timer.builder("transaction.processing.duration")
                .description("Transaction processing duration")
                .tag("service", "transaction-service")
                .register(registry);
    }
    
    @Bean
    public Counter idempotencyHitCounter(MeterRegistry registry) {
        return Counter.builder("idempotency.hit")
                .description("Number of idempotency key hits")
                .tag("service", "transaction-service")
                .register(registry);
    }
}
