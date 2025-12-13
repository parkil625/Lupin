package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.domain.enums.SocialProvider;
import com.example.demo.dto.LoginDto;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NaverOAuthService 테스트")
class NaverOAuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private RestClient restClient;

    private NaverOAuthService naverOAuthService;
    private User user;

    @BeforeEach
    void setUp() {
        naverOAuthService = new NaverOAuthService(
                userRepository, jwtTokenProvider, refreshTokenRepository, restClient);

        user = User.builder()
                .userId("test@example.com")
                .password("password")
                .name("테스트")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        ReflectionTestUtils.setField(naverOAuthService, "clientId", "testClientId");
        ReflectionTestUtils.setField(naverOAuthService, "clientSecret", "testClientSecret");
    }

    @SuppressWarnings("unchecked")
    private void mockTokenRequest(Map<String, Object> tokenResponse) {
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestBodySpec);
        given(requestBodySpec.contentType(any())).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(MultiValueMap.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(Map.class)).willReturn(tokenResponse);
    }

    @SuppressWarnings("unchecked")
    private void mockUserInfoRequest(Map<String, Object> userInfoResponse) {
        RestClient.RequestHeadersUriSpec requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        given(restClient.get()).willReturn(requestHeadersUriSpec);
        given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.header(anyString(), anyString())).willReturn(requestHeadersSpec);
        given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.body(Map.class)).willReturn(userInfoResponse);
    }

    @Test
    @DisplayName("네이버 로그인 성공")
    void naverLoginSuccessTest() {
        // given
        String code = "authCode";
        String state = "randomState";

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "naverAccessToken");
        mockTokenRequest(tokenResponse);

        Map<String, Object> userInfoResponse = new HashMap<>();
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("email", "test@example.com");
        responseData.put("id", "naverId123");
        userInfoResponse.put("response", responseData);
        mockUserInfoRequest(userInfoResponse);

        given(userRepository.findByProviderEmail("test@example.com")).willReturn(Optional.of(user));
        given(jwtTokenProvider.createAccessToken(anyString(), anyString())).willReturn("accessToken");
        given(jwtTokenProvider.createRefreshToken(anyString())).willReturn("refreshToken");
        given(jwtTokenProvider.getRefreshTokenValidityMs()).willReturn(604800000L);

        // when
        LoginDto result = naverOAuthService.naverLogin(code, state);

        // then
        assertThat(result.getUserId()).isEqualTo("test@example.com");
        assertThat(result.getAccessToken()).isEqualTo("accessToken");
    }

    @Test
    @DisplayName("네이버 로그인 실패 - 사용자 없음")
    void naverLoginUserNotFoundTest() {
        // given
        String code = "authCode";
        String state = "randomState";

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "naverAccessToken");
        mockTokenRequest(tokenResponse);

        Map<String, Object> userInfoResponse = new HashMap<>();
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("email", "unknown@example.com");
        responseData.put("id", "naverId123");
        userInfoResponse.put("response", responseData);
        mockUserInfoRequest(userInfoResponse);

        given(userRepository.findByProviderEmail("unknown@example.com")).willReturn(Optional.empty());
        given(userRepository.findByUserId("unknown@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> naverOAuthService.naverLogin(code, state))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("네이버 계정 연동 성공")
    void linkNaverSuccessTest() {
        // given
        String code = "authCode";
        String state = "randomState";

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "naverAccessToken");
        mockTokenRequest(tokenResponse);

        Map<String, Object> userInfoResponse = new HashMap<>();
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("email", "naver@example.com");
        responseData.put("id", "naverId123");
        userInfoResponse.put("response", responseData);
        mockUserInfoRequest(userInfoResponse);

        given(userRepository.findByProviderEmail("naver@example.com")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(user);

        // when
        naverOAuthService.linkNaver(user, code, state);

        // then
        assertThat(user.getProvider()).isEqualTo(SocialProvider.NAVER);
        assertThat(user.getProviderId()).isEqualTo("naverId123");
        assertThat(user.getProviderEmail()).isEqualTo("naver@example.com");
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("네이버 계정 연동 실패 - 이미 다른 계정에 연동됨")
    void linkNaverAlreadyUsedTest() {
        // given
        String code = "authCode";
        String state = "randomState";

        User otherUser = User.builder()
                .userId("other@example.com")
                .password("password")
                .name("다른사용자")
                .role(Role.MEMBER)
                .build();
        ReflectionTestUtils.setField(otherUser, "id", 2L);

        Map<String, Object> tokenResponse = new HashMap<>();
        tokenResponse.put("access_token", "naverAccessToken");
        mockTokenRequest(tokenResponse);

        Map<String, Object> userInfoResponse = new HashMap<>();
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("email", "already@example.com");
        responseData.put("id", "naverId123");
        userInfoResponse.put("response", responseData);
        mockUserInfoRequest(userInfoResponse);

        given(userRepository.findByProviderEmail("already@example.com")).willReturn(Optional.of(otherUser));

        // when & then
        assertThatThrownBy(() -> naverOAuthService.linkNaver(user, code, state))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_ALREADY_USED);
    }

    @Test
    @DisplayName("네이버 토큰 발급 실패")
    void naverLoginTokenErrorTest() {
        // given
        String code = "invalidCode";
        String state = "randomState";

        mockTokenRequest(null);

        // when & then
        assertThatThrownBy(() -> naverOAuthService.naverLogin(code, state))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.OAUTH_TOKEN_ERROR);
    }
}
