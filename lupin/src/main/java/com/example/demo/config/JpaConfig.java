package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // JPA Auditing 활성화
    // BaseEntity의 @CreatedDate, @LastModifiedDate 자동 설정
}
