package com.yourcompany.exportprocessor.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

    /**
     * Export API Retry instance.
     * Configured via application.yml resilience4j.retry.instances.export-api
     */
    @Bean
    public Retry exportApiRetry(RetryRegistry retryRegistry) {
        return retryRegistry.retry("export-api");
    }

    /**
     * Export API Circuit Breaker instance.
     * Configured via application.yml resilience4j.circuitbreaker.instances.export-api
     */
    @Bean
    public CircuitBreaker exportApiCircuitBreaker(CircuitBreakerRegistry circuitBreakerRegistry) {
        return circuitBreakerRegistry.circuitBreaker("export-api");
    }
}
