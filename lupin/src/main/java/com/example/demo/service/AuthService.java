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
        // 1. 이메일로 유저 조회
        User user = userRepository.findByUserId(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 3. 토큰 생성 및 반환
        return generateTokens(user);
    }

    /**
     * 구글 로그인
     */
    public LoginDto googleLogin(String googleIdToken) {
        try {
            // 1. 구글 ID Token 검증
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

            // 2. DB에서 이메일로 사용자 조회 (이메일 또는 userId로 조회)
            User user = userRepository.findByEmail(email)
                    .or(() -> userRepository.findByUserId(email))
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            // 3. 토큰 생성 및 반환
            return generateTokens(user);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("구글 로그인 실패", e);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    /**
     * 토큰 재발급 (RTR 적용)
     * - Access Token과 Refresh Token을 모두 새로 발급하여 반환
     */
    public LoginDto reissue(String refreshToken) {
        // 1. Refresh Token 유효성 검증 (서명 확인 등)
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 2. 토큰에서 User ID 추출
        String userId = jwtTokenProvider.getEmail(refreshToken);

        // 3. Redis에서 해당 유저의 저장된 Refresh Token 가져오기
        String storedRefreshToken = redisTemplate.opsForValue().get(userId);

        // 4. Redis에 토큰이 없거나(로그아웃/만료), 요청 토큰과 다르면(탈취 의심) 에러
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 5. 유저 조회
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 6. 새로운 토큰 세트 발급 (RTR)
        return generateTokens(user);
    }

    /**
     * 로그아웃
     */
    public void logout(String accessToken) {
        // 1. Access Token에서 "Bearer " 제거
        if (StringUtils.hasText(accessToken) && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7);
        }

        // 2. Access Token 검증 (이미 만료된 거면 굳이 처리 안 해도 됨)
        if (!jwtTokenProvider.validateToken(accessToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN); // 혹은 그냥 return;
        }

        // 3. User ID 추출 및 Refresh Token 삭제 (기존 로직)
        String userId = jwtTokenProvider.getEmail(accessToken);
        if (redisTemplate.opsForValue().get(userId) != null) {
            redisTemplate.delete(userId); // Refresh Token 삭제 (재발급 불가)
        }

        // 4. (New!) Access Token 남은 시간 계산해서 블랙리스트 등록
        // "이 토큰은 남은 수명(예: 20분)동안 절대 쓸 수 없다"라고 Redis에 "logout" 딱지 붙임
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

    // Private Helper Method: 토큰 생성 및 Redis 저장 (중복 제거)
    private LoginDto generateTokens(User user) {
        // 1. Access / Refresh Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getUserId());

        // 2. Redis에 Refresh Token 저장 (기존 값 덮어쓰기 -> RTR)
        redisTemplate.opsForValue().set(
                user.getUserId(),
                refreshToken,
                REFRESH_TOKEN_VALIDITY,
                TimeUnit.DAYS
        );

        // 3. DTO 반환 (이제 유저 정보도 꽉 채워서 보냅니다)
        return LoginDto.builder()
                .id(user.getId())              // PK
                .email(user.getUserId())       // 이메일(ID)
                .name(user.getRealName())      // 이름
                .role(user.getRole().name())   // 권한
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}