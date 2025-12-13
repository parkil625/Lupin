package com.example.demo.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * CORS 설정 프로퍼티
 * application.yml에서 app.cors.* 값을 타입 안전하게 관리
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    private static final String DEFAULT_ORIGIN = "http://localhost:3000";

    /**
     * 허용된 origin 목록 (쉼표로 구분된 문자열 또는 환경변수)
     */
    private String allowedOrigins;

    /**
     * 허용된 HTTP 메서드
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

    /**
     * 허용된 헤더
     */
    private List<String> allowedHeaders = List.of("*");

    /**
     * 자격 증명 허용 여부
     */
    private boolean allowCredentials = true;

    /**
     * Preflight 요청 캐시 시간 (초)
     */
    private long maxAge = 3600L;

    /**
     * 유효한 origin 목록 반환 (비어있으면 기본값 반환)
     */
    public List<String> getEffectiveOrigins() {
        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return List.of(DEFAULT_ORIGIN);
        }

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        return origins.isEmpty() ? List.of(DEFAULT_ORIGIN) : origins;
    }
}
