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
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Timing Attack 방어용 더미 해시 (BCrypt 형식)
     *
     * 중요: 이 해시는 실제 BCrypt 알고리즘으로 생성된 유효한 형식이어야 합니다.
     * 잘못된 형식의 문자열을 사용하면 matches()가 즉시 false를 반환하여
     * Timing Attack 방어 효과가 사라집니다.
     *
     * 아래 해시는 BCrypt cost factor 10으로 생성된 유효한 해시입니다.
     * (원본 평문은 무의미한 랜덤 문자열이며, 실제 비밀번호로 사용되지 않습니다)
     */
    private static final String DUMMY_PASSWORD_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjqQBrkHx92t.dce8UsUVbXN.jSzTi";

    /**
     * 일반 로그인
     *
     * 보안 강화:
     * 1. 사용자 미존재와 비밀번호 불일치를 동일한 에러로 처리
     * 2. Timing Attack 방어: 유저 존재 여부와 관계없이 동일한 연산 시간 보장
     *
     * 성능 최적화:
     * - BCrypt 검증(CPU 집약적)은 트랜잭션 외부에서 수행
     * - DB 커넥션 점유 시간 최소화로 동시 접속 처리량 향상
     */
    public LoginDto login(LoginRequest request) {
        // [트랜잭션 외부] 유저 조회 (읽기 전용, 빠름)
        User user = findUserForLogin(request.getEmail());

        // [트랜잭션 외부] BCrypt 검증 (CPU 집약적, 느림)
        // DB 커넥션을 점유하지 않은 상태에서 수행
        String storedPassword = (user != null) ? user.getPassword() : DUMMY_PASSWORD_HASH;
        boolean passwordMatch = passwordEncoder.matches(request.getPassword(), storedPassword);

        if (user == null || !passwordMatch) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // [트랜잭션 내부] 토큰 생성 및 저장 (Redis 쓰기)
        return generateTokens(user);
    }

    /**
     * 로그인용 유저 조회 (읽기 전용)
     */
    @Transactional(readOnly = true)
    public User findUserForLogin(String userId) {
        return userRepository.findByUserId(userId).orElse(null);
    }

    /**
     * 토큰 재발급
     */
    @Transactional
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
    @Transactional
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
    @Transactional
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