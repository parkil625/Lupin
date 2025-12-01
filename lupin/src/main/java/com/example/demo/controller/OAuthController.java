package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.LoginDto;
import com.example.demo.service.AuthService;
import com.example.demo.service.KakaoOAuthService;
import com.example.demo.service.NaverOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController extends BaseController {

    private final AuthService authService;
    private final NaverOAuthService naverOAuthService;
    private final KakaoOAuthService kakaoOAuthService;

    /**
     * 구글 계정 연동
     */
    @PostMapping("/google/link")
    public ResponseEntity<Map<String, Object>> linkGoogle(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request
    ) {
        User user = getCurrentUser(userDetails);
        String googleToken = request.get("token");

        authService.linkGoogle(user, googleToken);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "provider", "GOOGLE",
                "providerEmail", user.getProviderEmail() != null ? user.getProviderEmail() : "",
                "connectedAt", ""
        ));
    }

    /**
     * OAuth 연동 목록 조회
     * User 엔티티에 단일 provider 정보만 있으므로 리스트로 변환하여 반환
     */
    @GetMapping("/connections")
    public ResponseEntity<List<Map<String, Object>>> getConnections(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getCurrentUser(userDetails);
        List<Map<String, Object>> connections = new ArrayList<>();

        if (user.getProvider() != null && !user.getProvider().isEmpty()) {
            connections.add(Map.of(
                    "id", user.getId(),
                    "provider", user.getProvider(),
                    "providerEmail", user.getProviderEmail() != null ? user.getProviderEmail() : "",
                    "connectedAt", ""
            ));
        }

        return ResponseEntity.ok(connections);
    }

    /**
     * OAuth 연동 해제
     */
    @DeleteMapping("/connections/{provider}")
    public ResponseEntity<Void> unlinkOAuth(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String provider
    ) {
        User user = getCurrentUser(userDetails);
        authService.unlinkOAuth(user, provider);
        return ResponseEntity.ok().build();
    }

    /**
     * 네이버 로그인
     */
    @PostMapping("/naver/login")
    public ResponseEntity<LoginDto> naverLogin(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String state = request.get("state");
        LoginDto loginDto = naverOAuthService.naverLogin(code, state);
        return ResponseEntity.ok(loginDto);
    }

    /**
     * 네이버 계정 연동
     */
    @PostMapping("/naver/link")
    public ResponseEntity<Map<String, Object>> linkNaver(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request
    ) {
        User user = getCurrentUser(userDetails);
        String code = request.get("code");
        String state = request.get("state");

        naverOAuthService.linkNaver(user, code, state);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "provider", "NAVER",
                "providerEmail", user.getProviderEmail() != null ? user.getProviderEmail() : "",
                "connectedAt", ""
        ));
    }

    /**
     * 카카오 로그인
     */
    @PostMapping("/kakao/login")
    public ResponseEntity<LoginDto> kakaoLogin(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String redirectUri = request.get("redirectUri");
        LoginDto loginDto = kakaoOAuthService.kakaoLogin(code, redirectUri);
        return ResponseEntity.ok(loginDto);
    }

    /**
     * 카카오 계정 연동
     */
    @PostMapping("/kakao/link")
    public ResponseEntity<Map<String, Object>> linkKakao(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody Map<String, String> request
    ) {
        User user = getCurrentUser(userDetails);
        String code = request.get("code");
        String redirectUri = request.get("redirectUri");

        kakaoOAuthService.linkKakao(user, code, redirectUri);

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "provider", "KAKAO",
                "providerEmail", user.getProviderEmail() != null ? user.getProviderEmail() : "",
                "connectedAt", ""
        ));
    }
}
