package com.velocityx.user_service.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Custom Metrics Configuration for business monitoring
 */
@Configuration
public class MetricsConfig {

    @Bean
    public Counter signupCounter(MeterRegistry registry) {
        return Counter.builder("user.signup.total")
                .description("Total number of user signups")
                .tag("application", "user-service")
                .register(registry);
    }

    @Bean
    public Counter loginSuccessCounter(MeterRegistry registry) {
        return Counter.builder("user.login.success.total")
                .description("Total number of successful logins")
                .tag("application", "user-service")
                .register(registry);
    }

    @Bean
    public Counter loginFailureCounter(MeterRegistry registry) {
        return Counter.builder("user.login.failure.total")
                .description("Total number of failed logins")
                .tag("application", "user-service")
                .register(registry);
    }

    @Bean
    public Counter accountLockedCounter(MeterRegistry registry) {
        return Counter.builder("user.account.locked.total")
                .description("Total number of account lockouts")
                .tag("application", "user-service")
                .register(registry);
    }

    @Bean
    public Timer loginTimer(MeterRegistry registry) {
        return Timer.builder("user.login.duration")
                .description("Login operation duration")
                .tag("application", "user-service")
                .register(registry);
    }

    @Bean
    public Timer signupTimer(MeterRegistry registry) {
        return Timer.builder("user.signup.duration")
                .description("Signup operation duration")
                .tag("application", "user-service")
                .register(registry);
    }
}
