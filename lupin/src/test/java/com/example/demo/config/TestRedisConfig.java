package com.example.demo.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import redis.embedded.RedisServer;

@TestConfiguration
public class TestRedisConfig {

    private RedisServer redisServer;

    public TestRedisConfig() {
        // 포트 충돌 방지를 위해 63790 포트 사용
        this.redisServer = RedisServer.builder()
                .port(63790)
                .setting("maxmemory 128M")
                .build();
    }

    @PostConstruct
    public void postConstruct() {
        try {
            redisServer.start();
            // Spring이 이 포트를 사용하도록 설정
            System.setProperty("spring.data.redis.port", "63790");
            System.setProperty("spring.data.redis.host", "localhost");
        } catch (Exception e) {
            System.err.println("Embedded Redis start failed: " + e.getMessage());
        }
    }

    @PreDestroy
    public void preDestroy() {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }
}