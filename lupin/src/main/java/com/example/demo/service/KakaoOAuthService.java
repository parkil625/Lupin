package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.SocialProvider;
import com.example.demo.dto.LoginDto;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.oauth.AbstractOAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Transactional(readOnly = true)
public class KakaoOAuthService extends AbstractOAuthService {

    @Value("${kakao.client-id:}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    public KakaoOAuthService(
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenRepository refreshTokenRepository,
            RestClient restClient
    ) {
        super(userRepository, jwtTokenProvider, refreshTokenRepository, restClient);
    }

    /**
     * 카카오 로그인
     */
    public LoginDto kakaoLogin(String code, String redirectUri) {
        return login(code, redirectUri);
    }

    /**
     * 카카오 계정 연동
     */
    @Transactional
    public void linkKakao(User user, String code, String redirectUri) {
        link(user, code, redirectUri);
    }

    @Override
    protected SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    @Override
    protected String getTokenUrl() {
        return "https://kauth.kakao.com/oauth/token";
    }

    @Override
    protected String getUserInfoUrl() {
        return "https://kapi.kakao.com/v2/user/me";
    }

    @Override
    protected MultiValueMap<String, String> buildTokenParams(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isEmpty()) {
            params.add("client_secret", clientSecret);
        }
        params.add("redirect_uri", redirectUri);
        params.add("code", code);
        return params;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected OAuthUserInfo extractUserInfo(Map<String, Object> userInfo) {
        String kakaoId = String.valueOf(userInfo.get("id"));
        Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;
        return new OAuthUserInfo(kakaoId, email);
    }
}
