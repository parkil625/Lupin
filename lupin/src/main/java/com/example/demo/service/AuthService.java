package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.SocialProvider;
import com.example.demo.dto.LoginDto;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.RefreshTokenRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * 일반 로그인
     * 보안 강화: 사용자 미존재와 비밀번호 불일치를 동일한 에러로 처리
     */
    public LoginDto login(LoginRequest request) {
        User user = userRepository.findByUserId(request.getEmail()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
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
        String storedRefreshToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 저장된 토큰과 다르면 다른 기기에서 로그인한 것
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
        refreshTokenRepository.deleteByUserId(userId);

        long expiration = jwtTokenProvider.getExpiration(accessToken);
        refreshTokenRepository.addToBlacklist(accessToken, expiration);
    }

    /**
     * OAuth 연동 해제
     */
    public void unlinkOAuth(User user, SocialProvider provider) {
        // 현재 연동된 provider와 일치하는지 확인
        if (user.getProvider() != provider) {
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

        // JWT 토큰과 저장소 TTL을 동일한 설정값으로 일원화
        refreshTokenRepository.save(
                user.getUserId(),
                refreshToken,
                jwtTokenProvider.getRefreshTokenValidityMs()
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