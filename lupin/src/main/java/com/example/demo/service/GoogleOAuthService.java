package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.SocialProvider;
import com.example.demo.dto.LoginDto;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.util.RedisKeyUtils;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Google OAuth 서비스
 * Google은 ID Token 검증 방식을 사용하므로 AbstractOAuthService와 다른 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleOAuthService {

    private static final long REFRESH_TOKEN_VALIDITY = 7;

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(googleClientId)) {
            log.warn("GOOGLE_CLIENT_ID가 설정되지 않았습니다. 구글 로그인 기능이 정상 동작하지 않습니다.");
            return;
        }
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    /**
     * 구글 로그인
     */
    public LoginDto googleLogin(String googleIdToken) {
        GoogleIdToken.Payload payload = verifyAndGetPayload(googleIdToken);
        String email = payload.getEmail();

        User user = userRepository.findByProviderEmail(email)
                .or(() -> userRepository.findByUserId(email))
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return generateTokens(user);
    }

    /**
     * 구글 계정 연동
     */
    @Transactional
    public void linkGoogle(User user, String googleIdToken) {
        GoogleIdToken.Payload payload = verifyAndGetPayload(googleIdToken);
        String email = payload.getEmail();
        String googleId = payload.getSubject();

        // 이미 다른 계정에 연동된 이메일인지 확인
        userRepository.findByProviderEmail(email)
                .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                .ifPresent(existingUser -> {
                    throw new BusinessException(ErrorCode.OAUTH_ALREADY_USED);
                });

        // 연동 정보 업데이트
        user.linkOAuth(SocialProvider.GOOGLE.name(), googleId, email);
        userRepository.save(user);
    }

    private GoogleIdToken.Payload verifyAndGetPayload(String googleIdToken) {
        try {
            if (verifier == null) {
                throw new BusinessException(ErrorCode.OAUTH_TOKEN_ERROR);
            }

            GoogleIdToken idToken = verifier.verify(googleIdToken);
            if (idToken == null) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            return idToken.getPayload();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("구글 ID 토큰 검증 실패", e);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private LoginDto generateTokens(User user) {
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
}
