package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.LoginDto;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.exception.BusinessException;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserOAuthRepository userOAuthRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .userId("user01")
                .email("test@test.com")
                .password("encodedPassword")
                .realName("테스트유저")
                .role(Role.MEMBER)
                .build();
    }

    @Nested
    @DisplayName("일반 로그인")
    class Login {

        @Test
        @DisplayName("로그인 성공")
        void login_Success() {
            // given
            LoginRequest request = new LoginRequest("user01", "password");

            given(userRepository.findByUserId("user01")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("password", "encodedPassword")).willReturn(true);
            given(jwtTokenProvider.createAccessToken(anyString(), anyString())).willReturn("accessToken");
            given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("refreshToken");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);

            // when
            LoginDto result = authService.login(request);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo("user01");
            assertThat(result.getAccessToken()).isEqualTo("accessToken");
            assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
        }

        @Test
        @DisplayName("존재하지 않는 유저 로그인 실패")
        void login_UserNotFound_ThrowsException() {
            // given
            LoginRequest request = new LoginRequest("unknown", "password");
            given(userRepository.findByUserId("unknown")).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("비밀번호 불일치 로그인 실패")
        void login_InvalidPassword_ThrowsException() {
            // given
            LoginRequest request = new LoginRequest("user01", "wrongPassword");
            given(userRepository.findByUserId("user01")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("토큰 재발급")
    class Reissue {

        @Test
        @DisplayName("토큰 재발급 성공")
        void reissue_Success() {
            // given
            String refreshToken = "validRefreshToken";

            given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getEmail(refreshToken)).willReturn("user01");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("user01")).willReturn(refreshToken);
            given(userRepository.findByUserId("user01")).willReturn(Optional.of(user));
            given(jwtTokenProvider.createAccessToken(anyString(), anyString())).willReturn("newAccessToken");
            given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("newRefreshToken");

            // when
            LoginDto result = authService.reissue(refreshToken);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo("newAccessToken");
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 재발급 실패")
        void reissue_InvalidToken_ThrowsException() {
            // given
            String invalidToken = "invalidToken";
            given(jwtTokenProvider.validateToken(invalidToken)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.reissue(invalidToken))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("저장된 토큰과 불일치시 재발급 실패")
        void reissue_TokenMismatch_ThrowsException() {
            // given
            String refreshToken = "validToken";

            given(jwtTokenProvider.validateToken(refreshToken)).willReturn(true);
            given(jwtTokenProvider.getEmail(refreshToken)).willReturn("user01");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("user01")).willReturn("differentToken");

            // when & then
            assertThatThrownBy(() -> authService.reissue(refreshToken))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("로그아웃 성공")
        void logout_Success() {
            // given
            String accessToken = "Bearer validAccessToken";

            given(jwtTokenProvider.validateToken("validAccessToken")).willReturn(true);
            given(jwtTokenProvider.getEmail("validAccessToken")).willReturn("user01");
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("user01")).willReturn("refreshToken");
            given(jwtTokenProvider.getExpiration("validAccessToken")).willReturn(3600000L);

            // when
            authService.logout(accessToken);

            // then
            then(redisTemplate).should().delete("user01");
            then(valueOperations).should().set(eq("validAccessToken"), eq("logout"), anyLong(), any());
        }

        @Test
        @DisplayName("유효하지 않은 토큰으로 로그아웃 실패")
        void logout_InvalidToken_ThrowsException() {
            // given
            String accessToken = "Bearer invalidToken";
            given(jwtTokenProvider.validateToken("invalidToken")).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.logout(accessToken))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
