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
public class NaverOAuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${naver.client-id:}")
    private String clientId;

    @Value("${naver.client-secret:}")
    private String clientSecret;

    private final long REFRESH_TOKEN_VALIDITY = 7;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 네이버 로그인
     */
    public LoginDto naverLogin(String code, String state) {
        try {
            String accessToken = getAccessToken(code, state);
            Map<String, Object> userInfo = getUserInfo(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) userInfo.get("response");
            String email = (String) response.get("email");
            String naverId = (String) response.get("id");

            User user = userRepository.findByProviderEmail(email)
                    .or(() -> userRepository.findByUserId(email))
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            return generateTokens(user);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("네이버 로그인 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR);
        }
    }

    /**
     * 네이버 계정 연동
     */
    @Transactional // [최적화] DB 저장이 필요한 메서드만 쓰기 트랜잭션
    public void linkNaver(User user, String code, String state) {
        try {
            String accessToken = getAccessToken(code, state);
            Map<String, Object> userInfo = getUserInfo(accessToken);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = (Map<String, Object>) userInfo.get("response");
            String email = (String) response.get("email");
            String naverId = (String) response.get("id");

            // 이미 다른 계정에 연동된 이메일인지 확인
            userRepository.findByProviderEmail(email)
                    .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                    .ifPresent(existingUser -> {
                        throw new BusinessException(ErrorCode.OAUTH_ALREADY_USED);
                    });

            // 연동 정보 업데이트
            user.setProvider("NAVER");
            user.setProviderId(naverId);
            user.setProviderEmail(email);
            userRepository.save(user);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("네이버 계정 연동 실패", e);
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR);
        }
    }

    private String getAccessToken(String code, String state) {
        String tokenUrl = "https://nid.naver.com/oauth2.0/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("state", state);

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
        String userInfoUrl = "https://openapi.naver.com/v1/nid/me";

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
