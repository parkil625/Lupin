package com.example.demo.controller;

import com.example.demo.config.properties.CookieProperties;
import com.example.demo.dto.LoginDto;
import com.example.demo.dto.request.LoginRequest;
import com.example.demo.dto.response.LoginResponse;
import com.example.demo.service.AuthService;
import com.example.demo.service.GoogleOAuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * 인증 관련 API (Fixed & Perfect Version)
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";
    private static final String GOOGLE_TOKEN_KEY = "token";

    private final AuthService authService;
    private final GoogleOAuthService googleOAuthService;
    private final CookieProperties cookieProperties;

    /**
     * 일반 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginDto loginDto = authService.login(request);
        setRefreshTokenCookie(response, loginDto.getRefreshToken());
        return ResponseEntity.ok(LoginResponse.from(loginDto));
    }

    /**
     * 구글 로그인
     */
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String token = request.get(GOOGLE_TOKEN_KEY);
        LoginDto loginDto = googleOAuthService.googleLogin(token);
        setRefreshTokenCookie(response, loginDto.getRefreshToken());
        return ResponseEntity.ok(LoginResponse.from(loginDto));
    }

    /**
     * 토큰 재발급 (Reissue)
     */
    @PostMapping("/reissue")
    public ResponseEntity<LoginResponse> reissue(
            @CookieValue(value = REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        if (refreshToken == null) {
            return ResponseEntity.status(401).build();
        }
        LoginDto newTokens = authService.reissue(refreshToken);
        setRefreshTokenCookie(response, newTokens.getRefreshToken());
        return ResponseEntity.ok(LoginResponse.from(newTokens));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String accessToken,
            HttpServletResponse response
    ) {
        if (accessToken != null) {
            authService.logout(accessToken);
        }
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
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
        long maxAgeInSeconds = Duration.ofDays(cookieProperties.getRefreshTokenMaxAgeDays()).toSeconds();

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .path("/")
                .maxAge(maxAgeInSeconds)
                .sameSite(cookieProperties.getSameSite())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
