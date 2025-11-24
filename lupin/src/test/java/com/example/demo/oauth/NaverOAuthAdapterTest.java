package com.example.demo.oauth;

import com.example.demo.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@DisplayName("NaverOAuthAdapter 테스트")
class NaverOAuthAdapterTest {

    private NaverOAuthAdapter naverOAuthAdapter;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        naverOAuthAdapter = new NaverOAuthAdapter();
        restTemplate = mock(RestTemplate.class);

        ReflectionTestUtils.setField(naverOAuthAdapter, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(naverOAuthAdapter, "clientId", "testClientId");
        ReflectionTestUtils.setField(naverOAuthAdapter, "clientSecret", "testClientSecret");
    }

    @Test
    @DisplayName("프로바이더 이름 반환")
    void getProviderName_ReturnsNaver() {
        assertThat(naverOAuthAdapter.getProviderName()).isEqualTo("NAVER");
    }

    @Test
    @DisplayName("액세스 토큰 발급 성공")
    void getAccessToken_Success() {
        // given
        String tokenResponse = "{\"access_token\": \"test_access_token\", \"token_type\": \"bearer\"}";
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        // when
        String accessToken = naverOAuthAdapter.getAccessToken("authCode", "redirectUri");

        // then
        assertThat(accessToken).isEqualTo("test_access_token");
    }

    @Test
    @DisplayName("액세스 토큰 발급 실패 - 에러 응답")
    void getAccessToken_Error_ThrowsException() {
        // given
        String errorResponse = "{\"error\": \"invalid_request\", \"error_description\": \"Invalid code\"}";
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST));

        // when & then
        assertThatThrownBy(() -> naverOAuthAdapter.getAccessToken("invalidCode", "redirectUri"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("액세스 토큰 발급 실패 - 예외 발생")
    void getAccessToken_Exception_ThrowsBusinessException() {
        // given
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willThrow(new RuntimeException("Network error"));

        // when & then
        assertThatThrownBy(() -> naverOAuthAdapter.getAccessToken("authCode", "redirectUri"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("사용자 정보 조회 성공")
    void getUserInfo_Success() {
        // given
        String userInfoResponse = "{\"response\": {\"id\": \"12345\", \"email\": \"test@naver.com\", \"name\": \"테스트\"}}";
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

        // when
        OAuthUserInfo userInfo = naverOAuthAdapter.getUserInfo("accessToken");

        // then
        assertThat(userInfo.getId()).isEqualTo("12345");
        assertThat(userInfo.getEmail()).isEqualTo("test@naver.com");
        assertThat(userInfo.getName()).isEqualTo("테스트");
    }

    @Test
    @DisplayName("사용자 정보 조회 성공 - 이메일/이름 없음")
    void getUserInfo_NoEmailAndName_Success() {
        // given
        String userInfoResponse = "{\"response\": {\"id\": \"12345\"}}";
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

        // when
        OAuthUserInfo userInfo = naverOAuthAdapter.getUserInfo("accessToken");

        // then
        assertThat(userInfo.getId()).isEqualTo("12345");
        assertThat(userInfo.getEmail()).isNull();
        assertThat(userInfo.getName()).isNull();
    }

    @Test
    @DisplayName("사용자 정보 조회 실패")
    void getUserInfo_Exception_ThrowsBusinessException() {
        // given
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willThrow(new RuntimeException("API error"));

        // when & then
        assertThatThrownBy(() -> naverOAuthAdapter.getUserInfo("accessToken"))
                .isInstanceOf(BusinessException.class);
    }
}
