package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.dto.request.UserAvatarRequest;
import com.example.demo.dto.request.UserProfileRequest;
import com.example.demo.dto.response.DoctorResponse;
import com.example.demo.dto.response.UserRankingResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.dto.response.UserStatsResponse;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.PointService;
import com.example.demo.service.UserPenaltyService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final PointService pointService;
    private final UserPenaltyService userPenaltyService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @CurrentUser User user
    ) {
        long points = pointService.getTotalPoints(user);
        return ResponseEntity.ok(UserResponse.from(user, points));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<UserRankingResponse>> getTopUsersByPoints(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(userService.getTopUsersByPoints(limit));
    }

    @GetMapping("/{userId}/ranking-context")
    public ResponseEntity<List<UserRankingResponse>> getUserRankingContext(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(userService.getUserRankingContext(userId));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        long totalUsers = userService.getTotalUserCount();
        long activeUsersThisMonth = userService.getActiveUsersThisMonth();
        long averagePoints = userService.getAveragePoints();
        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "activeUsersThisMonth", activeUsersThisMonth,
                "averagePoints", averagePoints
        ));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserInfo(@PathVariable Long userId) {
        User user = userService.getUserInfo(userId);
        long points = pointService.getTotalPoints(user);
        return ResponseEntity.ok(UserResponse.from(user, points));
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserStats(userId));
    }

    @PutMapping("/me/avatar")
    public ResponseEntity<UserResponse> updateAvatar(
            @CurrentUser User currentUser,
            @RequestBody UserAvatarRequest request
    ) {
        User updatedUser = userService.updateAvatar(currentUser, request.getAvatar());
        long points = pointService.getTotalPoints(updatedUser);
        return ResponseEntity.ok(UserResponse.from(updatedUser, points));
    }

    @PutMapping("/me/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @CurrentUser User user,
            @Valid @RequestBody UserProfileRequest request
    ) {
        User updatedUser = userService.updateProfile(user, request.getName(), request.getHeight(), request.getWeight());
        long points = pointService.getTotalPoints(updatedUser);
        return ResponseEntity.ok(UserResponse.from(updatedUser, points));
    }

    @GetMapping("/points")
    public ResponseEntity<Map<String, Long>> getMyPoints(
            @CurrentUser User user
    ) {
        long totalPoints = pointService.getTotalPoints(user);
        return ResponseEntity.ok(Map.of("totalPoints", totalPoints));
    }

    @GetMapping("/points/monthly")
    public ResponseEntity<Map<String, Long>> getMonthlyPoints(
            @CurrentUser User user
    ) {
        long monthlyPoints = pointService.getMonthlyPoints(user, YearMonth.now());
        return ResponseEntity.ok(Map.of("monthlyPoints", monthlyPoints));
    }

    @GetMapping("/penalty")
    public ResponseEntity<Map<String, Boolean>> checkPenalty(
            @CurrentUser User user,
            @RequestParam PenaltyType type
    ) {
        boolean hasPenalty = userPenaltyService.hasActivePenalty(user, type);
        return ResponseEntity.ok(Map.of("hasPenalty", hasPenalty));
    }

    /**
     * 진료과별 의사 조회
     * GET /api/users/doctors?department=internal
     */
    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorResponse>> getDoctorsByDepartment(
            @RequestParam String department
    ) {
        List<User> doctors = userService.getDoctorsByDepartment(department);

        List<DoctorResponse> doctorList = doctors.stream()
                .map(DoctorResponse::from)
                .toList();

        return ResponseEntity.ok(doctorList);
    }

    /**
     * 모든 유저의 totalPoints를 point_logs에서 일괄 동기화
     * 반정규화 필드 초기 동기화 또는 복구용 (운영 시 관리자 권한 필요)
     */
    @PostMapping("/sync-points")
    public ResponseEntity<Map<String, Object>> syncAllUserTotalPoints() {
        int updatedCount = userService.syncAllUserTotalPoints();
        return ResponseEntity.ok(Map.of(
                "message", "User total points synced successfully",
                "updatedCount", updatedCount
        ));
    }
}
