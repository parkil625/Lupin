package com.example.demo.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 피드 관련 설정 프로퍼티
 * application.yml에서 app.feed.* 값을 타입 안전하게 관리
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.feed")
public class FeedProperties {

    /**
     * 피드 삭제 시 포인트 회수 가능 기간 (일)
     * 피드 생성 후 이 기간 내에 삭제하면 포인트가 회수됨
     */
    private int pointRecoveryDays = 7;
}
