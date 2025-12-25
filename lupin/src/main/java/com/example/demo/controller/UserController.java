package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.dto.request.UserAvatarRequest;
import com.example.demo.dto.request.UserProfileRequest;
import com.example.demo.dto.request.UserUpdateRequest;
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
    public ResponseEntity<UserResponse> getMe(@CurrentUser User user) {
        long points = pointService.getTotalPoints(user);
        // [수정] 피드 및 댓글 패널티 여부 조회하여 전달
        boolean hasFeedPenalty = userPenaltyService.hasActivePenalty(user, PenaltyType.FEED);
        boolean hasCommentPenalty = userPenaltyService.hasActivePenalty(user, PenaltyType.COMMENT);
        return ResponseEntity.ok(UserResponse.from(user, points, hasFeedPenalty, hasCommentPenalty));
    }

    @GetMapping("/ranking")
    public ResponseEntity<List<UserRankingResponse>> getTopUsersByPoints(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(userService.getTopUsersByPoints(limit));
    }

    @GetMapping("/{userId}/ranking-context")
    public ResponseEntity<List<UserRankingResponse>> getUserRankingContext(@PathVariable Long userId) {
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
        // [수정] 피드 및 댓글 패널티 여부 조회하여 전달
        boolean hasFeedPenalty = userPenaltyService.hasActivePenalty(user, PenaltyType.FEED);
        boolean hasCommentPenalty = userPenaltyService.hasActivePenalty(user, PenaltyType.COMMENT);
        return ResponseEntity.ok(UserResponse.from(user, points, hasFeedPenalty, hasCommentPenalty));
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

        // [수정] 서비스 호출 시 6개 파라미터 전달 (birthDate, gender 추가)
        userService.updateProfile(currentUser,
                request.getName(),
                request.getHeight(),
                request.getWeight(),
                request.getBirthDate(),
                request.getGender());

        User updatedUser = userService.getUserInfo(userId);
        long points = pointService.getTotalPoints(updatedUser);
        return ResponseEntity.ok(UserResponse.from(updatedUser, points));
    }

    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserStats(userId));
    }

    @PutMapping("/{userId}/avatar")
    public ResponseEntity<UserResponse> updateAvatar(
            @PathVariable Long userId,
            @CurrentUser User currentUser,
            @RequestBody UserAvatarRequest request
    ) {
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
        // [수정] 서비스 호출 시 6개 파라미터 전달
        userService.updateProfile(user, 
                request.getName(), 
                request.getHeight(), 
                request.getWeight(),
                request.getBirthDate(), 
                request.getGender());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/points")
    public ResponseEntity<Map<String, Long>> getMyPoints(@CurrentUser User user) {
        long totalPoints = pointService.getTotalPoints(user);
        return ResponseEntity.ok(Map.of("totalPoints", totalPoints));
    }

    @GetMapping("/points/monthly")
    public ResponseEntity<Map<String, Long>> getMonthlyPoints(@CurrentUser User user) {
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

    @GetMapping("/doctors")
    public ResponseEntity<List<DoctorResponse>> getDoctorsByDepartment(@RequestParam String department) {
        List<User> doctors = userService.getDoctorsByDepartment(department);
        List<DoctorResponse> doctorList = doctors.stream().map(DoctorResponse::from).toList();
        return ResponseEntity.ok(doctorList);
    }
}