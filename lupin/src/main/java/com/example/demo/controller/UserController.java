package com.example.demo.controller;

import com.example.demo.dto.response.UserProfileResponse;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 사용자 관련 API
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 사용자 단일 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable Long userId) {
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * 사용자 프로필 조회
     */
    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    /**
     * 포인트 적립
     */
    @PostMapping("/{userId}/points/add")
    public ResponseEntity<Void> addPoints(
            @PathVariable Long userId,
            @RequestParam Long amount,
            @RequestParam String reason,
            @RequestParam(required = false) String refId) {
        userService.addPoints(userId, amount, reason, refId);
        return ResponseEntity.ok().build();
    }

    /**
     * 포인트 차감
     */
    @PostMapping("/{userId}/points/deduct")
    public ResponseEntity<Void> deductPoints(
            @PathVariable Long userId,
            @RequestParam Long amount,
            @RequestParam String reason) {
        userService.deductPoints(userId, amount, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * 상위 포인트 사용자 조회 (랭킹)
     */
    @GetMapping("/top")
    public ResponseEntity<List<Map<String, Object>>> getTopUsersByPoints(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> topUsers = userService.getTopUsersByPoints(limit);
        return ResponseEntity.ok(topUsers);
    }

    /**
     * 특정 사용자 주변 랭킹 조회 (본인 + 앞뒤 1명)
     */
    @GetMapping("/{userId}/ranking/context")
    public ResponseEntity<List<Map<String, Object>>> getUserRankingContext(@PathVariable Long userId) {
        List<Map<String, Object>> rankingContext = userService.getUserRankingContext(userId);
        return ResponseEntity.ok(rankingContext);
    }

    /**
     * 전체 통계 조회
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> statistics = userService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
}
