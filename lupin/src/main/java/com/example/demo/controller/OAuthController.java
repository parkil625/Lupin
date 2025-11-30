package com.example.demo.controller;

import com.example.demo.domain.entity.User;
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
        // 현재 User 엔티티는 단일 provider만 지원하므로
        // 연동 해제는 지원하지 않음 (로그인 자체가 불가능해지므로)
        return ResponseEntity.ok().build();
    }
}
