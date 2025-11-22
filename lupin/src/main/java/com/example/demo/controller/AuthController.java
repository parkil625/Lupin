package com.example.demo.controller;

import com.example.demo.dto.LoginDto;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 인증 관련 API (Fixed & Perfect Version)
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 일반 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        // 1. Service는 DTO를 반환 (Access + Refresh 모두 포함)
        LoginDto loginDto = authService.login(request);

        // 2. Refresh Token은 HttpOnly 쿠키로 발급 (헬퍼 메서드 사용!)
        setRefreshTokenCookie(response, loginDto.getRefreshToken());

        // 3. Access Token만 Body로 반환 (보안 강화: Body에서 RefreshToken 제거 추천)
        return ResponseEntity.ok(LoginResponse.from(loginDto));
    }

    /**
     * 구글 로그인
     */
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String token = request.get("token");

        // 1. 구글 로그인도 DTO 반환
        LoginDto loginDto = authService.googleLogin(token);

        // 2. ★ 중요: 헬퍼 메서드 재사용 (쿠키 이름 "refresh_token" 통일됨)
        setRefreshTokenCookie(response, loginDto.getRefreshToken());

        // 3. 응답 반환
        return ResponseEntity.ok(LoginResponse.from(loginDto));
    }

    /**
     * 토큰 재발급 (Reissue)
     * - Access Token 만료 시 프론트엔드 인터셉터가 호출
     */
    @PostMapping("/reissue")
    public ResponseEntity<LoginResponse> reissue(
            @CookieValue(value = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            // 쿠키가 없으면 401 에러 -> 프론트에서 로그인 페이지로 리다이렉트
            return ResponseEntity.status(401).build();
        }

        // 1. 재발급 요청 (서비스에서 검증 및 RTR 수행 후 새 토큰 세트 반환)
        LoginDto newTokens = authService.reissue(refreshToken);

        // 2. ★ 중요: Refresh Token Rotation (RTR) - 사용한 쿠키는 버리고 새 쿠키 발급
        setRefreshTokenCookie(response, newTokens.getRefreshToken());

        // 3. Access Token 반환
        return ResponseEntity.ok(LoginResponse.from(newTokens));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = "Authorization", required = false) String accessToken,
            HttpServletResponse response
    ) {
        // 1. 로그아웃 로직 (Redis에 AccessToken 블랙리스트 추가 등)
        if (accessToken != null) {
            authService.logout(accessToken);
        }

        // 2. 쿠키 삭제 (Max-Age = 0)
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .maxAge(0)
                .path("/")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok().build();
    }

    // =========================================================================
    // Private Helper Method
    // =========================================================================
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)       // JS 접근 불가
                .secure(true)         // HTTPS 필수 (로컬에선 false 가능하지만 true 권장)
                .path("/")
                .maxAge(7 * 24 * 60 * 60) // 7일
                .sameSite("None")     // 크로스 도메인 요청 시 필수
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}