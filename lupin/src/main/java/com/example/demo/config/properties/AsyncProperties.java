package com.example.demo.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 비동기 스레드 풀 설정 프로퍼티
 * application.yml에서 app.async.* 값을 타입 안전하게 관리
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.async")
public class AsyncProperties {

    /**
     * 기본 스레드 수
     */
    private int corePoolSize = 5;

    /**
     * 최대 스레드 수
     */
    private int maxPoolSize = 10;

    /**
     * 대기 큐 용량
     */
    private int queueCapacity = 100;

    /**
     * 스레드 이름 접두사
     */
    private String threadNamePrefix = "AsyncThread-";
}
