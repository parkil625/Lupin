package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.LoginDto;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("test@example.com")
                .password("encodedPassword")
                .name("테스트")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("일반 로그인 성공")
    void loginSuccessTest() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "password");
        given(userRepository.findByUserId("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("password", "encodedPassword")).willReturn(true);
        given(jwtTokenProvider.createAccessToken(anyString(), anyString())).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("refreshToken");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        LoginDto result = authService.login(request);

        // then
        assertThat(result.getUserId()).isEqualTo("test@example.com");
        assertThat(result.getAccessToken()).isEqualTo("accessToken");
        assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 로그인하면 예외가 발생한다")
    void loginUserNotFoundTest() {
        // given
        LoginRequest request = new LoginRequest("unknown@example.com", "password");
        given(userRepository.findByUserId("unknown@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다")
    void loginInvalidPasswordTest() {
        // given
        LoginRequest request = new LoginRequest("test@example.com", "wrongPassword");
        given(userRepository.findByUserId("test@example.com")).willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PASSWORD);
    }

    @Test
    @DisplayName("토큰 재발급 성공")
    void reissueSuccessTest() {
        // given
        String refreshToken = "validRefreshToken";
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getEmail(refreshToken)).willReturn("test@example.com");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("test@example.com")).willReturn(refreshToken);
        given(userRepository.findByUserId("test@example.com")).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(anyString(), anyString())).willReturn("newAccessToken");
        given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("newRefreshToken");

        // when
        LoginDto result = authService.reissue(refreshToken);

        // then
        assertThat(result.getAccessToken()).isEqualTo("newAccessToken");
        assertThat(result.getRefreshToken()).isEqualTo("newRefreshToken");
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 재발급하면 예외가 발생한다")
    void reissueInvalidTokenTest() {
        // given
        String invalidToken = "invalidToken";
        given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.reissue(invalidToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    @DisplayName("Redis에 저장된 토큰과 다르면 다른 기기 로그인 예외가 발생한다")
    void reissueTokenMismatchTest() {
        // given
        String refreshToken = "validRefreshToken";
        given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getEmail(refreshToken)).willReturn("test@example.com");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("test@example.com")).willReturn("differentToken");

        // when & then
        assertThatThrownBy(() -> authService.reissue(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_EXPIRED_BY_OTHER_LOGIN);
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logoutSuccessTest() {
        // given
        String accessToken = "Bearer validAccessToken";
        given(jwtTokenProvider.validateToken("validAccessToken")).willReturn(true);
        given(jwtTokenProvider.getEmail("validAccessToken")).willReturn("test@example.com");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("test@example.com")).willReturn("someToken");
        given(jwtTokenProvider.getExpiration("validAccessToken")).willReturn(3600000L);

        // when
        authService.logout(accessToken);

        // then
        verify(redisTemplate).delete("test@example.com");
        verify(valueOperations).set(eq("validAccessToken"), eq("logout"), anyLong(), any());
    }

    @Test
    @DisplayName("유효하지 않은 토큰으로 로그아웃하면 예외가 발생한다")
    void logoutInvalidTokenTest() {
        // given
        String accessToken = "Bearer invalidToken";
        given(jwtTokenProvider.validateToken("invalidToken")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.logout(accessToken))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
    }

    @Test
    @DisplayName("OAuth 연동 해제 성공")
    void unlinkOAuthSuccessTest() {
        // given
        user.setProvider("GOOGLE");
        user.setProviderId("google123");
        user.setProviderEmail("test@gmail.com");
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        authService.unlinkOAuth(user, "GOOGLE");

        // then
        assertThat(user.getProvider()).isNull();
        assertThat(user.getProviderId()).isNull();
        assertThat(user.getProviderEmail()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("연동되지 않은 OAuth 해제 시 예외가 발생한다")
    void unlinkOAuthNotLinkedTest() {
        // given
        user.setProvider(null);

        // when & then
        assertThatThrownBy(() -> authService.unlinkOAuth(user, "GOOGLE"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_NOT_LINKED);
    }

    @Test
    @DisplayName("다른 Provider로 연동 해제 시 예외가 발생한다")
    void unlinkOAuthDifferentProviderTest() {
        // given
        user.setProvider("KAKAO");

        // when & then
        assertThatThrownBy(() -> authService.unlinkOAuth(user, "GOOGLE"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_NOT_LINKED);
    }
}
