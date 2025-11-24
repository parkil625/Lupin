package com.example.demo.config;

import com.example.demo.service.DistributedLockService;
import com.example.demo.service.RedisLuaService;
import com.example.demo.service.ResilientRedisService;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 테스트용 Redis Mock 설정
 */
@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    @Primary
    public RedisTemplate<String, String> redisTemplate() {
        return Mockito.mock(RedisTemplate.class);
    }

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        return Mockito.mock(RedissonClient.class);
    }

    @Bean
    @Primary
    public DistributedLockService distributedLockService() {
        DistributedLockService mock = Mockito.mock(DistributedLockService.class);

        // 모든 락 메서드가 실제 작업을 실행하도록 설정
        when(mock.executeWithLock(any(String.class), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());

        when(mock.withDrawLock(any(Long.class), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());

        when(mock.withPrizeStockLock(any(Long.class), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());

        when(mock.withChallengeJoinLock(any(Long.class), any(Long.class), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get());

        when(mock.withTicketIssueLock(any(Long.class), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());

        when(mock.withPointsLock(any(Long.class), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());

        when(mock.withFeedLikeLock(any(Long.class), any(Long.class), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(2)).get());

        return mock;
    }

    @Bean
    @Primary
    public RedisLuaService redisLuaService() {
        RedisLuaService mock = Mockito.mock(RedisLuaService.class);

        // 기본 동작 설정
        when(mock.joinChallengeAtomic(any(Long.class), any(Long.class))).thenReturn(true);
        when(mock.decrementStockAtomic(any(Long.class))).thenReturn(1L);
        when(mock.deductPointsAtomic(any(Long.class), any(Long.class))).thenReturn(100L);
        when(mock.toggleFeedLike(any(Long.class), any(Long.class))).thenReturn(1L);
        when(mock.issueTicketAtomic(any(Long.class))).thenReturn(true);
        when(mock.checkApiRateLimit(any(Long.class), any(String.class), any(Integer.class), any(Integer.class))).thenReturn(true);

        return mock;
    }

    @Bean
    @Primary
    public ResilientRedisService resilientRedisService() {
        return Mockito.mock(ResilientRedisService.class);
    }
}
