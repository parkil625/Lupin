package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.entity.UserOAuth;
import com.example.demo.dto.LoginDto;
import com.example.demo.dto.response.OAuthConnectionResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.oauth.OAuthProvider;
import com.example.demo.oauth.OAuthProviderFactory;
import com.example.demo.oauth.OAuthUserInfo;
import com.example.demo.repository.UserOAuthRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * OAuth Service with Factory + Adapter + Strategy Pattern
 * 확장성과 유지보수성을 극대화한 설계
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OAuthService {

    private final UserOAuthRepository userOAuthRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final OAuthProviderFactory providerFactory;

    private static final long REFRESH_TOKEN_VALIDITY = 7;

    /**
     * 통합 OAuth 로그인
     * @param providerName NAVER, KAKAO 등
     * @param code 인가 코드
     * @param redirectUri 리다이렉트 URI
     */
    public LoginDto login(String providerName, String code, String redirectUri) {
        OAuthProvider provider = providerFactory.getProvider(providerName);

        String accessToken = provider.getAccessToken(code, redirectUri);
        OAuthUserInfo userInfo = provider.getUserInfo(accessToken);

        UserOAuth userOAuth = userOAuthRepository
                .findByProviderAndProviderId(provider.getProviderName(), userInfo.getId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OAUTH_NOT_LINKED,
                        String.format("연동된 %s 계정이 없습니다. 먼저 계정을 연동해주세요.", providerName)
                ));

        return generateTokens(userOAuth.getUser());
    }

    /**
     * 통합 OAuth 계정 연동
     */
    public OAuthConnectionResponse linkAccount(String loginId, String providerName, String code, String redirectUri) {
        User user = userRepository.findByUserId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        OAuthProvider provider = providerFactory.getProvider(providerName);

        String accessToken = provider.getAccessToken(code, redirectUri);
        OAuthUserInfo userInfo = provider.getUserInfo(accessToken);

        // 이미 연동된 계정 검증
        validateNotAlreadyLinked(user.getId(), provider.getProviderName());
        validateNotUsedByOthers(provider.getProviderName(), userInfo.getId());

        UserOAuth userOAuth = UserOAuth.builder()
                .user(user)
                .provider(provider.getProviderName())
                .providerId(userInfo.getId())
                .providerEmail(userInfo.getEmail())
                .build();

        UserOAuth saved = userOAuthRepository.save(userOAuth);

        log.info("{} 계정 연동 완료 - loginId: {}, providerId: {}",
                providerName, loginId, userInfo.getId());

        return OAuthConnectionResponse.from(saved);
    }

    // === Legacy Methods (하위 호환성 유지) ===

    public LoginDto naverLogin(String code, String state) {
        return login("NAVER", code, state);
    }

    public LoginDto kakaoLogin(String code, String redirectUri) {
        return login("KAKAO", code, redirectUri);
    }

    public OAuthConnectionResponse linkNaverAccount(String loginId, String code, String state) {
        return linkAccount(loginId, "NAVER", code, state);
    }

    public OAuthConnectionResponse linkKakaoAccount(String loginId, String code, String redirectUri) {
        return linkAccount(loginId, "KAKAO", code, redirectUri);
    }

    // === Query Methods ===

    @Transactional(readOnly = true)
    public List<OAuthConnectionResponse> getConnectionsByLoginId(String loginId) {
        User user = userRepository.findByUserId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return userOAuthRepository.findByUserId(user.getId())
                .stream()
                .map(OAuthConnectionResponse::from)
                .toList();
    }

    public void unlinkOAuthByLoginId(String loginId, String provider) {
        User user = userRepository.findByUserId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String upperProvider = provider.toUpperCase();

        if (!userOAuthRepository.existsByUserIdAndProvider(user.getId(), upperProvider)) {
            throw new BusinessException(ErrorCode.OAUTH_NOT_LINKED,
                    "연동된 " + provider + " 계정이 없습니다.");
        }

        userOAuthRepository.deleteByUserIdAndProvider(user.getId(), upperProvider);
        log.info("OAuth 연동 해제 완료 - loginId: {}, provider: {}", loginId, provider);
    }

    /**
     * 지원하는 OAuth 프로바이더 목록
     */
    @Transactional(readOnly = true)
    public List<String> getSupportedProviders() {
        return providerFactory.getSupportedProviders();
    }

    // === Private Helper Methods ===

    private void validateNotAlreadyLinked(Long userId, String providerName) {
        if (userOAuthRepository.existsByUserIdAndProvider(userId, providerName)) {
            throw new BusinessException(ErrorCode.ALREADY_LINKED_OAUTH,
                    String.format("이미 %s 계정이 연동되어 있습니다.", providerName));
        }
    }

    private void validateNotUsedByOthers(String providerName, String providerId) {
        userOAuthRepository.findByProviderAndProviderId(providerName, providerId)
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.OAUTH_ALREADY_USED,
                            String.format("이미 다른 계정에 연동된 %s 계정입니다.", providerName));
                });
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
                .email(user.getUserId())
                .name(user.getRealName())
                .role(user.getRole().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
