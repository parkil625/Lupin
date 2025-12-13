package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.LoginDto;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    // Refresh Token 만료 시간 (7일)
    private static final long REFRESH_TOKEN_VALIDITY = 7;

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
     * 토큰 재발급
     */
    public LoginDto reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String userId = jwtTokenProvider.getEmail(refreshToken);
        String storedRefreshToken = redisTemplate.opsForValue().get(RedisKeyUtils.refreshToken(userId));

        // Redis에 저장된 토큰이 없으면 만료된 것
        if (storedRefreshToken == null) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // Redis에 다른 토큰이 있으면 다른 기기에서 로그인한 것
        if (!storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.SESSION_EXPIRED_BY_OTHER_LOGIN);
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
        String refreshTokenKey = RedisKeyUtils.refreshToken(userId);
        if (redisTemplate.opsForValue().get(refreshTokenKey) != null) {
            redisTemplate.delete(refreshTokenKey);
        }

        long expiration = jwtTokenProvider.getExpiration(accessToken);

        if (expiration > 0) {
            redisTemplate.opsForValue().set(
                    RedisKeyUtils.blacklist(accessToken),
                    "logout",
                    expiration,
                    TimeUnit.MILLISECONDS
            );
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
        user.unlinkOAuth();
        userRepository.save(user);
    }

    // Private Helper Method
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