package com.example.demo.repository;

import java.util.Optional;

/**
 * RefreshToken 저장소 추상화
 * - 인프라스트럭처(Redis) 의존성을 도메인 서비스로부터 분리
 * - 테스트 시 Mock으로 대체 가능
 */
public interface RefreshTokenRepository {

    /**
     * RefreshToken 저장
     *
     * @param userId 사용자 ID
     * @param refreshToken 리프레시 토큰
     * @param validityMs 유효 시간 (밀리초)
     */
    void save(String userId, String refreshToken, long validityMs);

    /**
     * RefreshToken 조회
     *
     * @param userId 사용자 ID
     * @return 저장된 리프레시 토큰 (없으면 empty)
     */
    Optional<String> findByUserId(String userId);

    /**
     * RefreshToken 삭제
     *
     * @param userId 사용자 ID
     */
    void deleteByUserId(String userId);

    /**
     * AccessToken을 블랙리스트에 추가 (로그아웃 처리)
     *
     * @param accessToken 액세스 토큰
     * @param expirationMs 남은 유효 시간 (밀리초)
     */
    void addToBlacklist(String accessToken, long expirationMs);
}
