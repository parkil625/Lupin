package com.example.demo.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // 1. [핵심] 실패한 기록이 있다면 복구(Repair)하여 에러를 없앰
            flyway.repair();
            // 2. 그 다음 정상적으로 마이그레이션 실행
            flyway.migrate();
        };
    }
}