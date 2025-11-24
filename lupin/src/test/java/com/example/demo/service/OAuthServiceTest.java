package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.UserOAuth;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.LoginDto;
import com.example.demo.dto.response.OAuthConnectionResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.oauth.OAuthProvider;
import com.example.demo.oauth.OAuthProviderFactory;
import com.example.demo.oauth.OAuthUserInfo;
import com.example.demo.repository.UserOAuthRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuthService 테스트")
class OAuthServiceTest {

    @InjectMocks
    private OAuthService oAuthService;

    @Mock
    private UserOAuthRepository userOAuthRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private OAuthProviderFactory providerFactory;
    @Mock
    private OAuthProvider oAuthProvider;

    private User user;
    private UserOAuth userOAuth;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("user01")
                .realName("테스트유저")
                .role(Role.MEMBER)
                .build();

        userOAuth = UserOAuth.builder()
                .id(1L)
                .user(user)
                .provider(com.example.demo.domain.enums.OAuthProvider.NAVER)
                .providerId("naver123")
                .providerEmail("test@naver.com")
                .build();
    }

    @Nested
    @DisplayName("OAuth 로그인")
    class Login {

        @Test
        @DisplayName("OAuth 로그인 성공")
        void login_Success() {
            // given
            OAuthUserInfo userInfo = new OAuthUserInfo("naver123", "test@naver.com", "테스트");

            given(providerFactory.getProvider("NAVER")).willReturn(oAuthProvider);
            given(oAuthProvider.getAccessToken("code", "state")).willReturn("accessToken");
            given(oAuthProvider.getUserInfo("accessToken")).willReturn(userInfo);
            given(oAuthProvider.getProviderName()).willReturn("NAVER");
            given(userOAuthRepository.findByProviderAndProviderId("NAVER", "naver123"))
                    .willReturn(Optional.of(userOAuth));
            given(jwtTokenProvider.createAccessToken(anyString(), anyString())).willReturn("accessToken");
            given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("refreshToken");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            LoginDto result = oAuthService.login("NAVER", "code", "state");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("accessToken");
        }

        @Test
        @DisplayName("연동되지 않은 계정 로그인 실패")
        void login_NotLinked_ThrowsException() {
            // given
            OAuthUserInfo userInfo = new OAuthUserInfo("unknown123", "test@naver.com", "테스트");

            given(providerFactory.getProvider("NAVER")).willReturn(oAuthProvider);
            given(oAuthProvider.getAccessToken("code", "state")).willReturn("accessToken");
            given(oAuthProvider.getUserInfo("accessToken")).willReturn(userInfo);
            given(oAuthProvider.getProviderName()).willReturn("NAVER");
            given(userOAuthRepository.findByProviderAndProviderId("NAVER", "unknown123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> oAuthService.login("NAVER", "code", "state"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("OAuth 계정 연동")
    class LinkAccount {

        @Test
        @DisplayName("계정 연동 성공")
        void linkAccount_Success() {
            // given
            OAuthUserInfo userInfo = new OAuthUserInfo("kakao123", "test@kakao.com", "테스트");

            given(userRepository.findByUserId("user01")).willReturn(Optional.of(user));
            given(providerFactory.getProvider("KAKAO")).willReturn(oAuthProvider);
            given(oAuthProvider.getAccessToken("code", "redirect")).willReturn("accessToken");
            given(oAuthProvider.getUserInfo("accessToken")).willReturn(userInfo);
            given(oAuthProvider.getProviderName()).willReturn("KAKAO");
            given(userOAuthRepository.existsByUserIdAndProvider(1L, "KAKAO")).willReturn(false);
            given(userOAuthRepository.findByProviderAndProviderId("KAKAO", "kakao123"))
                    .willReturn(Optional.empty());
            given(userOAuthRepository.save(any(UserOAuth.class))).willAnswer(invocation -> {
                UserOAuth saved = invocation.getArgument(0);
                ReflectionTestUtils.setField(saved, "id", 10L);
                return saved;
            });

            // when
            OAuthConnectionResponse result = oAuthService.linkAccount("user01", "KAKAO", "code", "redirect");

            // then
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("이미 연동된 계정 연동 실패")
        void linkAccount_AlreadyLinked_ThrowsException() {
            // given
            OAuthUserInfo userInfo = new OAuthUserInfo("kakao123", "test@kakao.com", "테스트");

            given(userRepository.findByUserId("user01")).willReturn(Optional.of(user));
            given(providerFactory.getProvider("KAKAO")).willReturn(oAuthProvider);
            given(oAuthProvider.getAccessToken("code", "redirect")).willReturn("accessToken");
            given(oAuthProvider.getUserInfo("accessToken")).willReturn(userInfo);
            given(oAuthProvider.getProviderName()).willReturn("KAKAO");
            given(userOAuthRepository.existsByUserIdAndProvider(1L, "KAKAO")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> oAuthService.linkAccount("user01", "KAKAO", "code", "redirect"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 유저 연동 실패")
        void linkAccount_UserNotFound_ThrowsException() {
            // given
            given(userRepository.findByUserId("unknown")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> oAuthService.linkAccount("unknown", "KAKAO", "code", "redirect"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("OAuth 연동 조회")
    class GetConnections {

        @Test
        @DisplayName("연동 목록 조회 성공")
        void getConnections_Success() {
            // given
            UserOAuth kakaoOAuth = UserOAuth.builder()
                    .id(2L)
                    .user(user)
                    .provider(com.example.demo.domain.enums.OAuthProvider.KAKAO)
                    .providerId("kakao123")
                    .providerEmail("test@kakao.com")
                    .build();

            given(userRepository.findByUserId("user01")).willReturn(Optional.of(user));
            given(userOAuthRepository.findByUserId(1L)).willReturn(Arrays.asList(userOAuth, kakaoOAuth));

            // when
            List<OAuthConnectionResponse> result = oAuthService.getConnectionsByLoginId("user01");

            // then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("OAuth 연동 해제")
    class UnlinkOAuth {

        @Test
        @DisplayName("연동 해제 성공")
        void unlinkOAuth_Success() {
            // given
            given(userRepository.findByUserId("user01")).willReturn(Optional.of(user));
            given(userOAuthRepository.existsByUserIdAndProvider(1L, "NAVER")).willReturn(true);

            // when
            oAuthService.unlinkOAuthByLoginId("user01", "naver");

            // then
            then(userOAuthRepository).should().deleteByUserIdAndProvider(1L, "NAVER");
        }

        @Test
        @DisplayName("연동되지 않은 계정 해제 실패")
        void unlinkOAuth_NotLinked_ThrowsException() {
            // given
            given(userRepository.findByUserId("user01")).willReturn(Optional.of(user));
            given(userOAuthRepository.existsByUserIdAndProvider(1L, "KAKAO")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> oAuthService.unlinkOAuthByLoginId("user01", "kakao"))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
