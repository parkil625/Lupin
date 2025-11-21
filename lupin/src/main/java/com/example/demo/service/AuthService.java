package com.example.demo.service;

import com.example.demo.domain.entity.MedicalStaff;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.MedicalStaffRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * 인증 관련 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final MedicalStaffRepository medicalStaffRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${GOOGLE_CLIENT_ID}")
    private String googleClientId;

    /**
     * 로그인
     */
    public LoginResponse login(LoginRequest request) {
        // 먼저 일반 사용자 테이블에서 조회
        var userOpt = userRepository.findByUserId(request.getEmail());

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 비밀번호 검증
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BusinessException(ErrorCode.INVALID_PASSWORD);
            }

            // JWT 토큰 생성
            String token = jwtTokenProvider.createToken(user.getUserId(), user.getRole().name());

            log.info("로그인 성공: userId={}, role={}", user.getUserId(), user.getRole());

            return LoginResponse.of(
                    token,
                    user.getId(),
                    user.getUserId(),
                    user.getRealName(),
                    user.getRole().name()
            );
        }

        // 일반 사용자가 없으면 의료진 테이블에서 조회
        MedicalStaff staff = medicalStaffRepository.findByUserId(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), staff.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // JWT 토큰 생성 (의료진은 DOCTOR 역할)
        String token = jwtTokenProvider.createToken(staff.getUserId(), "DOCTOR");

        log.info("의료진 로그인 성공: userId={}", staff.getUserId());

        return LoginResponse.of(
                token,
                staff.getId(),
                staff.getUserId(),
                staff.getRealName(),
                "DOCTOR"
        );
    }

    /**
     * 구글 로그인
     */
    @Transactional
    public LoginResponse googleLogin(String googleIdToken) {
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

            // 2. DB에서 이메일로 사용자 조회 (직원 확인)
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            // 3. JWT 토큰 생성
            String token = jwtTokenProvider.createToken(user.getUserId(), user.getRole().name());

            log.info("구글 로그인 성공: userId={}, email={}", user.getUserId(), email);

            // 5. 응답 반환
            return LoginResponse.of(
                    token,
                    user.getId(),
                    user.getUserId(),
                    user.getRealName(),
                    user.getRole().name()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("구글 로그인 실패", e);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}
