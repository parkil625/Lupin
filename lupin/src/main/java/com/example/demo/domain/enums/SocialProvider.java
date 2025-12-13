package com.example.demo.domain.enums;

/**
 * OAuth 소셜 로그인 제공자
 */
public enum SocialProvider {
    KAKAO,
    NAVER,
    GOOGLE;

    /**
     * 문자열로부터 SocialProvider 변환 (대소문자 무시)
     */
    public static SocialProvider fromString(String provider) {
        if (provider == null) {
            return null;
        }
        try {
            return SocialProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
