package com.example.demo.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "redisson.enabled", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {

    @Value("${redisson.single-server-config.address}")
    private String address;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress(address);
        return Redisson.create(config);
    }
}