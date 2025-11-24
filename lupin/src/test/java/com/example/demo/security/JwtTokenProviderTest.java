package com.example.demo.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // 테스트용 비밀키 (Base64 인코딩된 256비트 이상의 키)
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckp3dFRva2VuUHJvdmlkZXJUZXN0MTIzNDU2Nzg5MGFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey", TEST_SECRET);
        jwtTokenProvider.setKey(TEST_SECRET);
    }

    @Test
    @DisplayName("Access Token 생성 성공")
    void createAccessToken_Success() {
        // when
        String token = jwtTokenProvider.createAccessToken("user01", "MEMBER");

        // then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3); // JWT 형식 확인
    }

    @Test
    @DisplayName("Refresh Token 생성 성공")
    void createRefreshToken_Success() {
        // when
        String token = jwtTokenProvider.createRefreshToken("user01");

        // then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("토큰 유효성 검증 성공")
    void validateToken_Valid_ReturnsTrue() {
        // given
        String token = jwtTokenProvider.createAccessToken("user01", "MEMBER");

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰 검증 실패")
    void validateToken_Invalid_ReturnsFalse() {
        // given
        String invalidToken = "invalid.token.here";

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("빈 토큰 검증 실패")
    void validateToken_Empty_ReturnsFalse() {
        // when
        boolean isValid = jwtTokenProvider.validateToken("");

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("토큰에서 인증 정보 조회 성공")
    void getAuthentication_Success() {
        // given
        String token = jwtTokenProvider.createAccessToken("user01", "MEMBER");

        // when
        Authentication authentication = jwtTokenProvider.getAuthentication(token);

        // then
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("user01");
        assertThat(authentication.getAuthorities()).isNotEmpty();
    }

    @Test
    @DisplayName("토큰에서 이메일 추출 성공")
    void getEmail_Success() {
        // given
        String token = jwtTokenProvider.createAccessToken("user01@test.com", "MEMBER");

        // when
        String email = jwtTokenProvider.getEmail(token);

        // then
        assertThat(email).isEqualTo("user01@test.com");
    }

    @Test
    @DisplayName("토큰 만료 시간 조회 성공")
    void getExpiration_Success() {
        // given
        String token = jwtTokenProvider.createAccessToken("user01", "MEMBER");

        // when
        long expiration = jwtTokenProvider.getExpiration(token);

        // then
        assertThat(expiration).isGreaterThan(0);
        assertThat(expiration).isLessThanOrEqualTo(1000 * 60 * 30); // 30분 이하
    }

    @Test
    @DisplayName("ADMIN 권한으로 토큰 생성")
    void createAccessToken_AdminRole_Success() {
        // given
        String token = jwtTokenProvider.createAccessToken("admin", "ADMIN");

        // when
        Authentication authentication = jwtTokenProvider.getAuthentication(token);

        // then
        assertThat(authentication.getAuthorities())
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
    }
}
