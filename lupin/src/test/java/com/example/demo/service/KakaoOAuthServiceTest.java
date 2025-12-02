package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.LoginDto;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("KakaoOAuthService 테스트")
class KakaoOAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private KakaoOAuthService kakaoOAuthService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId("test@example.com")
                .password("password")
                .name("테스트")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        // RestTemplate 주입
        ReflectionTestUtils.setField(kakaoOAuthService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(kakaoOAuthService, "clientId", "testClientId");
        ReflectionTestUtils.setField(kakaoOAuthService, "clientSecret", "testClientSecret");
    }

    @Test
    @DisplayName("카카오 로그인 성공 - 이메일로 사용자 조회")
    void kakaoLoginWithEmailSuccessTest() {
        // given
        String code = "authCode";
        String redirectUri = "http://localhost:3000/callback";

        // 토큰 응답 mock
        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "kakaoAccessToken");
        given(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .willReturn(tokenResponse);

        // 사용자 정보 응답 mock
        Map<String, Object> userInfoResponse = new HashMap<>();
        userInfoResponse.put("id", 12345L);
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "test@example.com");
        userInfoResponse.put("kakao_account", kakaoAccount);
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .willReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

        given(userRepository.findByProviderEmail("test@example.com")).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(anyString(), anyString())).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("refreshToken");
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        LoginDto result = kakaoOAuthService.kakaoLogin(code, redirectUri);

        // then
        assertThat(result.getUserId()).isEqualTo("test@example.com");
        assertThat(result.getAccessToken()).isEqualTo("accessToken");
    }

    @Test
    @DisplayName("카카오 로그인 실패 - 사용자 없음")
    void kakaoLoginUserNotFoundTest() {
        // given
        String code = "authCode";
        String redirectUri = "http://localhost:3000/callback";

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "kakaoAccessToken");
        given(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .willReturn(tokenResponse);

        Map<String, Object> userInfoResponse = new HashMap<>();
        userInfoResponse.put("id", 12345L);
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "unknown@example.com");
        userInfoResponse.put("kakao_account", kakaoAccount);
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .willReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

        given(userRepository.findByProviderEmail("unknown@example.com")).willReturn(Optional.empty());
        given(userRepository.findByUserId("unknown@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> kakaoOAuthService.kakaoLogin(code, redirectUri))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("카카오 계정 연동 성공")
    void linkKakaoSuccessTest() {
        // given
        String code = "authCode";
        String redirectUri = "http://localhost:3000/callback";

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "kakaoAccessToken");
        given(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .willReturn(tokenResponse);

        Map<String, Object> userInfoResponse = new HashMap<>();
        userInfoResponse.put("id", 12345L);
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "kakao@example.com");
        userInfoResponse.put("kakao_account", kakaoAccount);
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .willReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

        given(userRepository.findByProviderEmail("kakao@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        kakaoOAuthService.linkKakao(user, code, redirectUri);

        // then
        assertThat(user.getProvider()).isEqualTo("KAKAO");
        assertThat(user.getProviderId()).isEqualTo("12345");
        assertThat(user.getProviderEmail()).isEqualTo("kakao@example.com");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("카카오 계정 연동 실패 - 이미 다른 계정에 연동됨")
    void linkKakaoAlreadyUsedTest() {
        // given
        String code = "authCode";
        String redirectUri = "http://localhost:3000/callback";

        User otherUser = User.builder()
                .userId("other@example.com")
                .password("password")
                .name("다른사용자")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(otherUser, "id", 2L);

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "kakaoAccessToken");
        given(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .willReturn(tokenResponse);

        Map<String, Object> userInfoResponse = new HashMap<>();
        userInfoResponse.put("id", 12345L);
        Map<String, Object> kakaoAccount = new HashMap<>();
        kakaoAccount.put("email", "already@example.com");
        userInfoResponse.put("kakao_account", kakaoAccount);
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .willReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

        given(userRepository.findByProviderEmail("already@example.com")).willReturn(Optional.of(otherUser));

        // when & then
        assertThatThrownBy(() -> kakaoOAuthService.linkKakao(user, code, redirectUri))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ALREADY_USED);
    }

    @Test
    @DisplayName("카카오 토큰 발급 실패")
    void kakaoLoginTokenErrorTest() {
        // given
        String code = "invalidCode";
        String redirectUri = "http://localhost:3000/callback";

        given(restTemplate.postForObject(anyString(), any(HttpEntity.class), eq(Map.class)))
                .willReturn(null);

        // when & then
        assertThatThrownBy(() -> kakaoOAuthService.kakaoLogin(code, redirectUri))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_TOKEN_ERROR);
    }
}
