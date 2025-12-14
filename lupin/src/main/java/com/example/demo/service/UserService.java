package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.dto.response.UserRankingResponse;
import com.example.demo.dto.response.UserStatsResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PointLogRepository pointLogRepository;
    private final FeedRepository feedRepository;
    private final CommentRepository commentRepository;

    public User getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void updateProfile(User user, String name, Double height, Double weight) {
        user.updateProfile(name, height, weight);
    }

    public List<UserRankingResponse> getTopUsersByPoints(int limit) {
        List<Object[]> results = pointLogRepository.findAllUsersWithPointsRanked(PageRequest.of(0, limit));
        List<UserRankingResponse> rankings = new ArrayList<>();
        int rank = 1;
        for (Object[] row : results) {
            User user = (User) row[0];
            Long points = (Long) row[1];
            rankings.add(UserRankingResponse.of(user, points != null ? points : 0L, rank++));
        }
        return rankings;
    }

    public List<UserRankingResponse> getUserRankingContext(Long userId) {
        List<Object[]> results = pointLogRepository.findUserRankingContext(userId);
        return results.stream()
                .map(UserRankingResponse::fromNativeQuery)
                .toList();
    }

    public long getTotalUserCount() {
        return userRepository.count();
    }

    public long getActiveUsersThisMonth() {
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        Long count = pointLogRepository.countActiveUsersThisMonth(startOfMonth, endOfMonth);
        return count != null ? count : 0L;
    }

    public long getAveragePoints() {
        Double avg = pointLogRepository.getAveragePointsPerUser();
        return avg != null ? Math.round(avg) : 0L;
    }

    public UserStatsResponse getUserStats(Long userId) {
        User user = getUserInfo(userId);
        long feedCount = feedRepository.countByWriterId(userId);
        long commentCount = commentRepository.countByWriterId(userId);
        return UserStatsResponse.of(userId, user.getTotalPoints(), feedCount, commentCount);
    }

    @Transactional
    public void updateAvatar(User user, String avatarUrl) {
        user.updateAvatar(avatarUrl);
        userRepository.save(user);
    }

    /**
     * 진료과별 의사 목록 조회
     */
    public List<User> getDoctorsByDepartment(String department) {
        return userRepository.findByRoleAndDepartment(Role.DOCTOR, department);
    }

    /**
     * 모든 유저의 totalPoints를 point_logs에서 일괄 동기화
     * 반정규화 필드 초기 동기화 또는 복구용
     */
    @Transactional
    public int syncAllUserTotalPoints() {
        int updatedCount = userRepository.syncAllUserTotalPoints();
        log.info("Synced totalPoints for {} users", updatedCount);
        return updatedCount;
    }
}
