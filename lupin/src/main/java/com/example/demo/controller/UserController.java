package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.dto.request.UserAvatarRequest;
import com.example.demo.dto.request.UserProfileRequest;
import com.example.demo.dto.request.UserUpdateRequest;
import com.example.demo.dto.response.DoctorResponse;
import com.example.demo.dto.response.UserResponse;
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
    public ResponseEntity<List<Map<String, Object>>> getTopUsersByPoints(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(userService.getTopUsersByPoints(limit));
    }

    @GetMapping("/{userId}/ranking-context")
    public ResponseEntity<List<Map<String, Object>>> getUserRankingContext(
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

    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @CurrentUser User currentUser,
            @RequestBody UserUpdateRequest request
    ) {
        // 본인만 수정 가능
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        userService.updateProfile(currentUser,
                request.getName(),
                request.getHeight(),
                request.getWeight());

        User updatedUser = userService.getUserInfo(userId);
        long points = pointService.getTotalPoints(updatedUser);
        return ResponseEntity.ok(UserResponse.from(updatedUser, points));
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserStats(userId));
    }

    @PutMapping("/{userId}/avatar")
    public ResponseEntity<UserResponse> updateAvatar(
            @PathVariable Long userId,
            @CurrentUser User currentUser,
            @RequestBody UserAvatarRequest request
    ) {
        // 본인만 수정 가능
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        userService.updateAvatar(currentUser, request.getAvatar());

        User updatedUser = userService.getUserInfo(userId);
        long points = pointService.getTotalPoints(updatedUser);
        return ResponseEntity.ok(UserResponse.from(updatedUser, points));
    }

    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(
            @CurrentUser User user,
            @Valid @RequestBody UserProfileRequest request
    ) {
        userService.updateProfile(user, request.getName(), request.getHeight(), request.getWeight());
        return ResponseEntity.ok().build();
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
}
