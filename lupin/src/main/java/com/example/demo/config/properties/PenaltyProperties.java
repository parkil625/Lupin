package com.example.demo.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 패널티 관련 설정 프로퍼티
 * application.yml에서 app.penalty.* 값을 타입 안전하게 관리
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.penalty")
public class PenaltyProperties {

    /**
     * 패널티 지속 기간 (일)
     */
    private int durationDays = 3;

    /**
     * 패널티 적용 임계값 배수
     * (신고 수 >= 좋아요 수 * multiplier) 일 때 패널티 적용
     */
    private int thresholdMultiplier = 5;
}
