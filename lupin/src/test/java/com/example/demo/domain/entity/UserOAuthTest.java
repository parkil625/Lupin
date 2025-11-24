package com.example.demo.domain.entity;

import com.example.demo.domain.enums.OAuthProvider;
import com.example.demo.domain.enums.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("UserOAuth 엔티티 테스트")
class UserOAuthTest {

    @Test
    @DisplayName("Google OAuth 연동 생성")
    void createGoogleOAuth_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user@test.com")
                .password("password")
                .realName("테스터")
                .role(Role.MEMBER)
                .build();

        // when
        UserOAuth userOAuth = UserOAuth.builder()
                .id(1L)
                .user(user)
                .provider(OAuthProvider.GOOGLE)
                .providerId("google123")
                .providerEmail("google@gmail.com")
                .build();

        // then
        assertThat(userOAuth.getProvider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(userOAuth.getProviderId()).isEqualTo("google123");
        assertThat(userOAuth.getProviderEmail()).isEqualTo("google@gmail.com");
    }

    @Test
    @DisplayName("Naver OAuth 연동 생성")
    void createNaverOAuth_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user@test.com")
                .password("password")
                .realName("테스터")
                .role(Role.MEMBER)
                .build();

        // when
        UserOAuth userOAuth = UserOAuth.builder()
                .id(2L)
                .user(user)
                .provider(OAuthProvider.NAVER)
                .providerId("naver456")
                .providerEmail("naver@naver.com")
                .build();

        // then
        assertThat(userOAuth.getProvider()).isEqualTo(OAuthProvider.NAVER);
    }

    @Test
    @DisplayName("Kakao OAuth 연동 생성")
    void createKakaoOAuth_Success() {
        // given
        User user = User.builder()
                .id(1L)
                .userId("user01")
                .email("user@test.com")
                .password("password")
                .realName("테스터")
                .role(Role.MEMBER)
                .build();

        // when
        UserOAuth userOAuth = UserOAuth.builder()
                .id(3L)
                .user(user)
                .provider(OAuthProvider.KAKAO)
                .providerId("kakao789")
                .providerEmail("kakao@kakao.com")
                .build();

        // then
        assertThat(userOAuth.getProvider()).isEqualTo(OAuthProvider.KAKAO);
    }

    @Test
    @DisplayName("이메일 업데이트")
    void updateProviderEmail_Success() {
        // given
        UserOAuth userOAuth = UserOAuth.builder()
                .id(1L)
                .provider(OAuthProvider.GOOGLE)
                .providerId("google123")
                .providerEmail("old@gmail.com")
                .build();

        // when
        userOAuth.updateProviderEmail("new@gmail.com");

        // then
        assertThat(userOAuth.getProviderEmail()).isEqualTo("new@gmail.com");
    }

    @Test
    @DisplayName("이메일 없이 생성")
    void createWithoutEmail_Success() {
        // given & when
        UserOAuth userOAuth = UserOAuth.builder()
                .id(1L)
                .provider(OAuthProvider.GOOGLE)
                .providerId("google123")
                .build();

        // then
        assertThat(userOAuth.getProviderEmail()).isNull();
    }
}
