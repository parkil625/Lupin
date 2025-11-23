package com.example.demo.controller;

import com.example.demo.dto.LoginDto;
import com.example.demo.dto.response.OAuthConnectionResponse;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.OAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 네이버 계정 연동
     */
    @PostMapping("/naver/link")
    public ResponseEntity<OAuthConnectionResponse> linkNaverAccount(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> request) {

        String loginId = getLoginIdFromToken(token);
        String code = request.get("code");
        String state = request.get("state");

        OAuthConnectionResponse response = oAuthService.linkNaverAccount(loginId, code, state);
        return ResponseEntity.ok(response);
    }

    /**
     * 네이버 로그인
     */
    @PostMapping("/naver/login")
    public ResponseEntity<LoginDto> naverLogin(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String state = request.get("state");

        LoginDto response = oAuthService.naverLogin(code, state);
        return ResponseEntity.ok(response);
    }

    /**
     * 카카오 계정 연동
     */
    @PostMapping("/kakao/link")
    public ResponseEntity<OAuthConnectionResponse> linkKakaoAccount(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> request) {

        String loginId = getLoginIdFromToken(token);
        String code = request.get("code");
        String redirectUri = request.get("redirectUri");

        OAuthConnectionResponse response = oAuthService.linkKakaoAccount(loginId, code, redirectUri);
        return ResponseEntity.ok(response);
    }

    /**
     * 카카오 로그인
     */
    @PostMapping("/kakao/login")
    public ResponseEntity<LoginDto> kakaoLogin(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String redirectUri = request.get("redirectUri");

        LoginDto response = oAuthService.kakaoLogin(code, redirectUri);
        return ResponseEntity.ok(response);
    }

    /**
     * OAuth 연동 목록 조회
     */
    @GetMapping("/connections")
    public ResponseEntity<List<OAuthConnectionResponse>> getConnections(
            @RequestHeader("Authorization") String token) {

        String loginId = getLoginIdFromToken(token);
        List<OAuthConnectionResponse> connections = oAuthService.getConnectionsByLoginId(loginId);
        return ResponseEntity.ok(connections);
    }

    /**
     * OAuth 연동 해제
     */
    @DeleteMapping("/connections/{provider}")
    public ResponseEntity<Void> unlinkOAuth(
            @RequestHeader("Authorization") String token,
            @PathVariable String provider) {

        String loginId = getLoginIdFromToken(token);
        oAuthService.unlinkOAuthByLoginId(loginId, provider);
        return ResponseEntity.ok().build();
    }

    // Helper method
    private String getLoginIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return jwtTokenProvider.getEmail(token);
    }
}
