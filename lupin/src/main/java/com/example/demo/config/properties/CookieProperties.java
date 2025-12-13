package com.example.demo.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 쿠키 관련 설정 프로퍼티
 * application.yml에서 app.cookie.* 값을 타입 안전하게 관리
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {

    /**
     * Secure 쿠키 사용 여부
     * 운영(HTTPS): true
     * 개발(HTTP): false
     */
    private boolean secure = true;

    /**
     * SameSite 설정
     * "Strict", "Lax", "None"
     */
    private String sameSite = "None";

    /**
     * Refresh Token 쿠키 만료 시간 (일)
     */
    private int refreshTokenMaxAgeDays = 7;
}
