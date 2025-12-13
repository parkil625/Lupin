package com.example.demo.service;

import com.example.demo.config.CacheConfig;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.Role;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Cacheable(value = CacheConfig.RANKING_CACHE, key = "#limit")
    public List<Map<String, Object>> getTopUsersByPoints(int limit) {
        // 모든 사용자를 포함 (PointLog가 없어도 0점으로 포함)
        List<Object[]> results = pointLogRepository.findAllUsersWithPointsRanked(PageRequest.of(0, limit));
        List<Map<String, Object>> rankings = new ArrayList<>();
        int rank = 1;
        for (Object[] row : results) {
            User user = (User) row[0];
            Long points = (Long) row[1];
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", user.getId());
            entry.put("name", user.getName());
            entry.put("avatar", user.getAvatar());
            entry.put("department", user.getDepartment());
            entry.put("points", points);
            entry.put("rank", rank++);
            rankings.add(entry);
        }
        return rankings;
    }

    @Cacheable(value = CacheConfig.USER_RANKING_CONTEXT_CACHE, key = "#userId")
    public List<Map<String, Object>> getUserRankingContext(Long userId) {
        // Window Function을 사용하여 해당 사용자와 앞뒤 사용자만 조회 (메모리 효율적)
        List<Object[]> results = pointLogRepository.findUserRankingContext(userId);

        List<Map<String, Object>> context = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", ((Number) row[0]).longValue());
            entry.put("name", row[1]);
            entry.put("avatar", row[2]);
            entry.put("department", row[3]);
            entry.put("points", ((Number) row[4]).longValue());
            entry.put("rank", ((Number) row[5]).intValue());
            context.add(entry);
        }
        return context;
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

    public Map<String, Object> getUserStats(Long userId) {
        User user = getUserInfo(userId);
        Long totalPoints = pointLogRepository.sumPointsByUser(user);
        long feedCount = feedRepository.countByWriterId(userId);
        long commentCount = commentRepository.countByWriterId(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", userId);
        stats.put("totalPoints", totalPoints != null ? totalPoints : 0L);
        stats.put("feedCount", feedCount);
        stats.put("commentCount", commentCount);
        return stats;
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
}
