package com.example.demo.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Circuit Breaker 설정
 * - Redis 장애 시 자동 폴백
 * - 재시도 정책
 */
@Configuration
public class Resilience4jConfig {

    // ==================== Circuit Breaker 설정 ====================

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)                // 실패율 50% 이상이면 OPEN
                .slowCallRateThreshold(50)               // 느린 호출 50% 이상이면 OPEN
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .waitDurationInOpenState(Duration.ofSeconds(30))  // OPEN 상태 30초 유지
                .permittedNumberOfCallsInHalfOpenState(3)         // HALF_OPEN에서 3회 테스트
                .minimumNumberOfCalls(5)                          // 최소 5회 호출 후 판단
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)                             // 최근 10회 기준
                .build();

        return CircuitBreakerRegistry.of(defaultConfig);
    }

    @Bean
    public CircuitBreaker redisCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("redis");
    }

    @Bean
    public CircuitBreaker dbCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("database");
    }

    // ==================== Retry 설정 ====================

    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig defaultConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .retryExceptions(Exception.class)
                .ignoreExceptions(IllegalArgumentException.class)
                .build();

        return RetryRegistry.of(defaultConfig);
    }

    @Bean
    public Retry redisRetry(RetryRegistry registry) {
        return registry.retry("redis");
    }
}
