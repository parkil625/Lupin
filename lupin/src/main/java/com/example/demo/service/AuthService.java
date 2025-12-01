package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.LoginDto;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    // Refresh Token 만료 시간 (7일)
    private final long REFRESH_TOKEN_VALIDITY = 7;

    /**
     * 일반 로그인
     */
    public LoginDto login(LoginRequest request) {
        User user = userRepository.findByUserId(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        return generateTokens(user);
    }

    /**
     * 구글 로그인
     */
    public LoginDto googleLogin(String googleIdToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleIdToken);
            if (idToken == null) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();

            User user = userRepository.findByProviderEmail(email)
                    .or(() -> userRepository.findByUserId(email))
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            return generateTokens(user);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("구글 로그인 실패", e);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * 토큰 재발급
     */
    public LoginDto reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String userId = jwtTokenProvider.getEmail(refreshToken);
        String storedRefreshToken = redisTemplate.opsForValue().get(userId);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return generateTokens(user);
    }

    /**
     * 로그아웃
     */
    public void logout(String accessToken) {
        if (StringUtils.hasText(accessToken) && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String userId = jwtTokenProvider.getEmail(accessToken);
        if (redisTemplate.opsForValue().get(userId) != null) {
            redisTemplate.delete(userId);
        }

        long expiration = jwtTokenProvider.getExpiration(accessToken);

        if (expiration > 0) {
            redisTemplate.opsForValue().set(
                    accessToken,
                    "logout",
                    expiration,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * 구글 계정 연동
     */
    public void linkGoogle(User user, String googleIdToken) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleIdToken);
            if (idToken == null) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String googleId = payload.getSubject();

            // 이미 다른 계정에 연동된 이메일인지 확인
            userRepository.findByProviderEmail(email)
                    .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                    .ifPresent(existingUser -> {
                        throw new BusinessException(ErrorCode.OAUTH_ALREADY_USED);
                    });

            // 연동 정보 업데이트
            user.setProvider("GOOGLE");
            user.setProviderId(googleId);
            user.setProviderEmail(email);
            userRepository.save(user);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("구글 계정 연동 실패", e);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * OAuth 연동 해제
     */
    public void unlinkOAuth(User user, String provider) {
        // 현재 연동된 provider와 일치하는지 확인
        if (user.getProvider() == null || !user.getProvider().equalsIgnoreCase(provider)) {
            throw new BusinessException(ErrorCode.OAUTH_NOT_LINKED);
        }

        // 연동 정보 제거
        user.setProvider(null);
        user.setProviderId(null);
        user.setProviderEmail(null);
        userRepository.save(user);
    }

    // Private Helper Method
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
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}