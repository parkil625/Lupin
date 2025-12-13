package com.example.demo.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Resilience4j Circuit Breaker 설정
 * - 설정값은 application.yml의 resilience4j.* 에서 관리
 * - Spring Boot Auto-Configuration이 CircuitBreakerRegistry, RetryRegistry 생성
 */
@Configuration
@RequiredArgsConstructor
public class Resilience4jConfig {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    @Bean
    public CircuitBreaker redisCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("redis");
    }

    @Bean
    public CircuitBreaker dbCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("database");
    }

    @Bean
    public Retry redisRetry() {
        return retryRegistry.retry("redis");
    }
}
