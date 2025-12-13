package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.SocialProvider;
import com.example.demo.dto.LoginDto;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.oauth.AbstractOAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Map;

@Service
@Transactional(readOnly = true)
public class NaverOAuthService extends AbstractOAuthService {

    @Value("${naver.client-id:}")
    private String clientId;

    @Value("${naver.client-secret:}")
    private String clientSecret;

    public NaverOAuthService(
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            RedisTemplate<String, String> redisTemplate
    ) {
        super(userRepository, jwtTokenProvider, redisTemplate);
    }

    /**
     * 네이버 로그인
     */
    public LoginDto naverLogin(String code, String state) {
        return login(code, state);
    }

    /**
     * 네이버 계정 연동
     */
    @Transactional
    public void linkNaver(User user, String code, String state) {
        link(user, code, state);
    }

    @Override
    protected String getProviderName() {
        return SocialProvider.NAVER.name();
    }

    @Override
    protected String getTokenUrl() {
        return "https://nid.naver.com/oauth2.0/token";
    }

    @Override
    protected String getUserInfoUrl() {
        return "https://openapi.naver.com/v1/nid/me";
    }

    @Override
    protected MultiValueMap<String, String> buildTokenParams(String code, String state) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("state", state);
        return params;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected OAuthUserInfo extractUserInfo(Map<String, Object> userInfo) {
        Map<String, Object> response = (Map<String, Object>) userInfo.get("response");
        String email = (String) response.get("email");
        String naverId = (String) response.get("id");
        return new OAuthUserInfo(naverId, email);
    }
}
