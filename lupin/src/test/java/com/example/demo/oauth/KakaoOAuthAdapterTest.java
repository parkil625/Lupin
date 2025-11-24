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

@DisplayName("KakaoOAuthAdapter 테스트")
class KakaoOAuthAdapterTest {

    private KakaoOAuthAdapter kakaoOAuthAdapter;
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        kakaoOAuthAdapter = new KakaoOAuthAdapter();
        restTemplate = mock(RestTemplate.class);

        ReflectionTestUtils.setField(kakaoOAuthAdapter, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(kakaoOAuthAdapter, "clientId", "testClientId");
    }

    @Test
    @DisplayName("프로바이더 이름 반환")
    void getProviderName_ReturnsKakao() {
        assertThat(kakaoOAuthAdapter.getProviderName()).isEqualTo("KAKAO");
    }

    @Test
    @DisplayName("액세스 토큰 발급 성공")
    void getAccessToken_Success() {
        // given
        String tokenResponse = "{\"access_token\": \"kakao_access_token\", \"token_type\": \"bearer\"}";
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>(tokenResponse, HttpStatus.OK));

        // when
        String accessToken = kakaoOAuthAdapter.getAccessToken("authCode", "http://localhost/callback");

        // then
        assertThat(accessToken).isEqualTo("kakao_access_token");
    }

    @Test
    @DisplayName("액세스 토큰 발급 실패 - 에러 응답")
    void getAccessToken_Error_ThrowsException() {
        // given
        String errorResponse = "{\"error\": \"invalid_grant\", \"error_description\": \"authorization code not found\"}";
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST));

        // when & then
        assertThatThrownBy(() -> kakaoOAuthAdapter.getAccessToken("invalidCode", "http://localhost/callback"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("액세스 토큰 발급 실패 - 예외 발생")
    void getAccessToken_Exception_ThrowsBusinessException() {
        // given
        given(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                .willThrow(new RuntimeException("Connection refused"));

        // when & then
        assertThatThrownBy(() -> kakaoOAuthAdapter.getAccessToken("authCode", "http://localhost/callback"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("사용자 정보 조회 성공")
    void getUserInfo_Success() {
        // given
        String userInfoResponse = "{\"id\": \"123456789\", \"kakao_account\": {\"email\": \"test@kakao.com\", \"profile\": {\"nickname\": \"카카오유저\"}}}";
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

        // when
        OAuthUserInfo userInfo = kakaoOAuthAdapter.getUserInfo("accessToken");

        // then
        assertThat(userInfo.getId()).isEqualTo("123456789");
        assertThat(userInfo.getEmail()).isEqualTo("test@kakao.com");
        assertThat(userInfo.getName()).isEqualTo("카카오유저");
    }

    @Test
    @DisplayName("사용자 정보 조회 성공 - 이메일/닉네임 없음")
    void getUserInfo_NoEmailAndNickname_Success() {
        // given
        String userInfoResponse = "{\"id\": \"123456789\"}";
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

        // when
        OAuthUserInfo userInfo = kakaoOAuthAdapter.getUserInfo("accessToken");

        // then
        assertThat(userInfo.getId()).isEqualTo("123456789");
        assertThat(userInfo.getEmail()).isNull();
        assertThat(userInfo.getName()).isNull();
    }

    @Test
    @DisplayName("사용자 정보 조회 성공 - kakao_account만 있고 프로필 없음")
    void getUserInfo_NoProfile_Success() {
        // given
        String userInfoResponse = "{\"id\": \"123456789\", \"kakao_account\": {\"email\": \"test@kakao.com\"}}";
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willReturn(new ResponseEntity<>(userInfoResponse, HttpStatus.OK));

        // when
        OAuthUserInfo userInfo = kakaoOAuthAdapter.getUserInfo("accessToken");

        // then
        assertThat(userInfo.getId()).isEqualTo("123456789");
        assertThat(userInfo.getEmail()).isEqualTo("test@kakao.com");
        assertThat(userInfo.getName()).isNull();
    }

    @Test
    @DisplayName("사용자 정보 조회 실패")
    void getUserInfo_Exception_ThrowsBusinessException() {
        // given
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .willThrow(new RuntimeException("API error"));

        // when & then
        assertThatThrownBy(() -> kakaoOAuthAdapter.getUserInfo("accessToken"))
                .isInstanceOf(BusinessException.class);
    }
}
