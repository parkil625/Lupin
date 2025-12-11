package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.LoginDto;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // [최적화] 기본 읽기 전용
public class KakaoOAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${kakao.client-id:}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    private final long REFRESH_TOKEN_VALIDITY = 7;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 카카오 로그인
     */
    public LoginDto kakaoLogin(String code, String redirectUri) {
        try {
            String accessToken = getAccessToken(code, redirectUri);
            Map<String, Object> userInfo = getUserInfo(accessToken);

            String kakaoId = String.valueOf(userInfo.get("id"));
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
            String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

            User user;
            if (email != null) {
                user = userRepository.findByProviderEmail(email)
                        .or(() -> userRepository.findByUserId(email))
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            } else {
                user = userRepository.findByProviderAndProviderId("KAKAO", kakaoId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            }

            return generateTokens(user);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 로그인 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR);
        }
    }

    /**
     * 카카오 계정 연동
     */
    @Transactional // [최적화] DB 저장이 필요한 메서드만 쓰기 트랜잭션
    public void linkKakao(User user, String code, String redirectUri) {
        try {
            String accessToken = getAccessToken(code, redirectUri);
            Map<String, Object> userInfo = getUserInfo(accessToken);

            String kakaoId = String.valueOf(userInfo.get("id"));
            @SuppressWarnings("unchecked")
            Map<String, Object> kakaoAccount = (Map<String, Object>) userInfo.get("kakao_account");
            String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

            // 이미 다른 계정에 연동된 이메일인지 확인
            if (email != null) {
                userRepository.findByProviderEmail(email)
                        .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                        .ifPresent(existingUser -> {
                            throw new BusinessException(ErrorCode.OAUTH_ALREADY_USED);
                        });
            }

            // 연동 정보 업데이트
            user.setProvider("KAKAO");
            user.setProviderId(kakaoId);
            user.setProviderEmail(email);
            userRepository.save(user);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("카카오 계정 연동 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR);
        }
    }

    private String getAccessToken(String code, String redirectUri) {
        String tokenUrl = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        if (clientSecret != null && !clientSecret.isEmpty()) {
            params.add("client_secret", clientSecret);
        }
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(tokenUrl, request, Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR);
        }

        return (String) response.get("access_token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getUserInfo(String accessToken) {
        String userInfoUrl = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                userInfoUrl, HttpMethod.GET, request, Map.class);

        if (response.getBody() == null) {
            throw new BusinessException(ErrorCode.OAUTH_USER_INFO_ERROR);
        }

        return response.getBody();
    }

    private LoginDto generateTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        redisTemplate.opsForValue().set(
                user.getUserId(),
                refreshToken,
                REFRESH_TOKEN_VALIDITY,
                TimeUnit.DAYS
        );

        return LoginDto.builder()
                .id(user.getId())
                .userId(user.getUserId())
                .email(user.getProviderEmail())
                .name(user.getName())
                .department(user.getDepartment())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
