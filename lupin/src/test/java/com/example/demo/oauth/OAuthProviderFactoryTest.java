package com.example.demo.oauth;

import com.example.demo.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@DisplayName("OAuthProviderFactory 테스트")
class OAuthProviderFactoryTest {

    private OAuthProviderFactory factory;
    private OAuthProvider naverProvider;
    private OAuthProvider kakaoProvider;

    @BeforeEach
    void setUp() {
        naverProvider = mock(OAuthProvider.class);
        kakaoProvider = mock(OAuthProvider.class);

        given(naverProvider.getProviderName()).willReturn("NAVER");
        given(kakaoProvider.getProviderName()).willReturn("KAKAO");

        factory = new OAuthProviderFactory(Arrays.asList(naverProvider, kakaoProvider));
    }

    @Test
    @DisplayName("네이버 프로바이더 조회 성공")
    void getProvider_Naver_Success() {
        // when
        OAuthProvider result = factory.getProvider("NAVER");

        // then
        assertThat(result).isEqualTo(naverProvider);
    }

    @Test
    @DisplayName("카카오 프로바이더 조회 성공")
    void getProvider_Kakao_Success() {
        // when
        OAuthProvider result = factory.getProvider("KAKAO");

        // then
        assertThat(result).isEqualTo(kakaoProvider);
    }

    @Test
    @DisplayName("소문자로 프로바이더 조회 성공")
    void getProvider_LowerCase_Success() {
        // when
        OAuthProvider result = factory.getProvider("naver");

        // then
        assertThat(result).isEqualTo(naverProvider);
    }

    @Test
    @DisplayName("지원하지 않는 프로바이더 조회 시 예외")
    void getProvider_Unsupported_ThrowsException() {
        // when & then
        assertThatThrownBy(() -> factory.getProvider("GOOGLE"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("지원하지 않는 OAuth 프로바이더");
    }

    @Test
    @DisplayName("지원 프로바이더 목록 조회")
    void getSupportedProviders_Success() {
        // when
        List<String> providers = factory.getSupportedProviders();

        // then
        assertThat(providers).containsExactly("KAKAO", "NAVER");
    }
}
