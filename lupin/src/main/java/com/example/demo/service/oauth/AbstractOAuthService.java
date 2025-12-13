package com.example.demo.service.oauth;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.LoginDto;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.util.RedisKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OAuth 서비스 공통 로직을 담은 추상 클래스 (Template Method 패턴)
 */
@Slf4j
public abstract class AbstractOAuthService {

    protected static final long REFRESH_TOKEN_VALIDITY = 7;

    protected final UserRepository userRepository;
    protected final JwtTokenProvider jwtTokenProvider;
    protected final RedisTemplate<String, String> redisTemplate;
    protected final RestTemplate restTemplate;

    protected AbstractOAuthService(
            UserRepository userRepository,
            JwtTokenProvider jwtTokenProvider,
            RedisTemplate<String, String> redisTemplate
    ) {
        this.userRepository = userRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        this.restTemplate = new RestTemplate();
    }

    /**
     * OAuth 로그인 Template Method
     */
    public LoginDto login(String code, String stateOrRedirectUri) {
        try {
            String accessToken = getAccessToken(code, stateOrRedirectUri);
            Map<String, Object> userInfo = getUserInfo(accessToken);

            OAuthUserInfo oauthUser = extractUserInfo(userInfo);

            User user = findUser(oauthUser);
            return generateTokens(user);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} 로그인 실패", getProviderName(), e);
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR);
        }
    }

    /**
     * OAuth 계정 연동 Template Method
     */
    public void link(User user, String code, String stateOrRedirectUri) {
        try {
            String accessToken = getAccessToken(code, stateOrRedirectUri);
            Map<String, Object> userInfo = getUserInfo(accessToken);

            OAuthUserInfo oauthUser = extractUserInfo(userInfo);

            // 이미 다른 계정에 연동된 이메일인지 확인
            if (oauthUser.email() != null) {
                userRepository.findByProviderEmail(oauthUser.email())
                        .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                        .ifPresent(existingUser -> {
                            throw new BusinessException(ErrorCode.OAUTH_ALREADY_USED);
                        });
            }

            // 연동 정보 업데이트
            user.linkOAuth(getProviderName(), oauthUser.providerId(), oauthUser.email());
            userRepository.save(user);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} 계정 연동 실패", getProviderName(), e);
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR);
        }
    }

    // ===================== Abstract Methods (Provider별 구현 필요) =====================

    /**
     * OAuth 제공자 이름 (KAKAO, NAVER 등)
     */
    protected abstract String getProviderName();

    /**
     * 토큰 요청 URL
     */
    protected abstract String getTokenUrl();

    /**
     * 사용자 정보 요청 URL
     */
    protected abstract String getUserInfoUrl();

    /**
     * 토큰 요청 파라미터 생성
     */
    protected abstract MultiValueMap<String, String> buildTokenParams(String code, String stateOrRedirectUri);

    /**
     * 사용자 정보 추출
     */
    protected abstract OAuthUserInfo extractUserInfo(Map<String, Object> userInfo);

    // ===================== Template Methods (공통 구현) =====================

    protected String getAccessToken(String code, String stateOrRedirectUri) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = buildTokenParams(code, stateOrRedirectUri);
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restTemplate.postForObject(getTokenUrl(), request, Map.class);

        if (response == null || response.get("access_token") == null) {
            throw new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR);
        }

        return (String) response.get("access_token");
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                getUserInfoUrl(), HttpMethod.GET, request, Map.class);

        if (response.getBody() == null) {
            throw new BusinessException(ErrorCode.OAUTH_USER_INFO_ERROR);
        }

        return response.getBody();
    }

    /**
     * 사용자 찾기 기본 구현 (email 우선, 없으면 providerId로 검색)
     */
    protected User findUser(OAuthUserInfo oauthUser) {
        if (oauthUser.email() != null) {
            return userRepository.findByProviderEmail(oauthUser.email())
                    .or(() -> userRepository.findByUserId(oauthUser.email()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        }
        return userRepository.findByProviderAndProviderId(getProviderName(), oauthUser.providerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    protected LoginDto generateTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        redisTemplate.opsForValue().set(
                RedisKeyUtils.refreshToken(user.getUserId()),
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

    /**
     * OAuth 사용자 정보 DTO
     */
    public record OAuthUserInfo(String providerId, String email) {}
}
