package com.example.demo.controller;

import com.example.demo.dto.response.UserProfileResponse;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 관련 API
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
}
