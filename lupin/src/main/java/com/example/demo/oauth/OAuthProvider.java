package com.example.demo.oauth;

/**
 * OAuth 프로바이더 공통 인터페이스 (어댑터 패턴)
 */
public interface OAuthProvider {

    /**
     * 프로바이더 이름 반환
     */
    String getProviderName();

    /**
     * 인가 코드로 액세스 토큰 발급
     */
    String getAccessToken(String code, String redirectUri);

    /**
     * 액세스 토큰으로 사용자 정보 조회
     */
    OAuthUserInfo getUserInfo(String accessToken);
}
