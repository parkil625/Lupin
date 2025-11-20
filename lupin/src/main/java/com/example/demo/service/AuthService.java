package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그인
     */
    public LoginResponse login(LoginRequest request) {
        // 사용자 조회 (userId로 로그인)
        User user = userRepository.findByUserId(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

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
}
