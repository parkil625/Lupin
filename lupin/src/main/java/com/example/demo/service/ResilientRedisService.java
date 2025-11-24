package com.example.demo.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Circuit Breaker + Retry가 적용된 Redis 서비스
 * - Redis 장애 시 폴백 처리
 * - 자동 재시도
 */
@Service
@Slf4j
public class ResilientRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ResilientRedisService(
            RedisTemplate<String, String> redisTemplate,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this.redisTemplate = redisTemplate;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("redis");
        this.retry = retryRegistry.retry("redis");
    }

    // ==================== 기본 연산 (Circuit Breaker 적용) ====================

    /**
     * GET with fallback
     */
    public String get(String key, String fallback) {
        Supplier<String> supplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> redisTemplate.opsForValue().get(key)
        );

        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            String result = supplier.get();
            return result != null ? result : fallback;
        } catch (Exception e) {
            log.error("Redis GET 실패, 폴백 반환: key={}, error={}", key, e.getMessage());
            return fallback;
        }
    }

    /**
     * GET as Long with fallback
     */
    public Long getAsLong(String key, Long fallback) {
        String value = get(key, null);
        if (value == null) return fallback;

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /**
     * SET with circuit breaker
     */
    public boolean set(String key, String value) {
        Supplier<Boolean> supplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> {
                    redisTemplate.opsForValue().set(key, value);
                    return true;
                }
        );

        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Redis SET 실패: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * INCREMENT with fallback
     */
    public Long increment(String key, Long fallback) {
        Supplier<Long> supplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> redisTemplate.opsForValue().increment(key)
        );

        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Redis INCREMENT 실패, 폴백 반환: key={}, error={}", key, e.getMessage());
            return fallback;
        }
    }

    /**
     * DECREMENT with fallback
     */
    public Long decrement(String key, Long fallback) {
        Supplier<Long> supplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> redisTemplate.opsForValue().decrement(key)
        );

        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Redis DECREMENT 실패, 폴백 반환: key={}, error={}", key, e.getMessage());
            return fallback;
        }
    }

    // ==================== Set 연산 ====================

    /**
     * SADD with circuit breaker
     */
    public boolean sadd(String key, String value) {
        Supplier<Boolean> supplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> {
                    Long result = redisTemplate.opsForSet().add(key, value);
                    return result != null && result > 0;
                }
        );

        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Redis SADD 실패: key={}, error={}", key, e.getMessage());
            return false;
        }
    }

    /**
     * SISMEMBER with fallback
     */
    public boolean sismember(String key, String value, boolean fallback) {
        Supplier<Boolean> supplier = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                () -> Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, value))
        );

        supplier = Retry.decorateSupplier(retry, supplier);

        try {
            return supplier.get();
        } catch (Exception e) {
            log.error("Redis SISMEMBER 실패, 폴백 반환: key={}, error={}", key, e.getMessage());
            return fallback;
        }
    }

    // ==================== 상태 조회 ====================

    /**
     * Circuit Breaker 상태 조회
     */
    public String getCircuitBreakerState() {
        return circuitBreaker.getState().name();
    }

    /**
     * Circuit Breaker 메트릭 조회
     */
    public CircuitBreaker.Metrics getMetrics() {
        return circuitBreaker.getMetrics();
    }
}
