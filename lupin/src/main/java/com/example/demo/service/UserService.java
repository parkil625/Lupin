package com.example.demo.service;

import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    public User getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void updateProfile(User user, String name, Double height, Double weight) {
        user.updateProfile(name, height, weight);
    }

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

    public List<Map<String, Object>> getUserRankingContext(Long userId) {
        // 모든 사용자를 포함 (PointLog가 없어도 0점으로 포함)
        List<Object[]> allRankings = pointLogRepository.findAllUsersWithPointsRankedAll();
        int userRank = -1;
        for (int i = 0; i < allRankings.size(); i++) {
            User u = (User) allRankings.get(i)[0];
            if (u.getId().equals(userId)) {
                userRank = i;
                break;
            }
        }

        List<Map<String, Object>> context = new ArrayList<>();
        if (userRank == -1) {
            // 이 경우는 발생하지 않아야 함 (모든 사용자가 포함되므로)
            User user = getUserInfo(userId);
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", user.getId());
            entry.put("name", user.getName());
            entry.put("avatar", user.getAvatar());
            entry.put("department", user.getDepartment());
            entry.put("points", 0L);
            entry.put("rank", allRankings.size() + 1);
            context.add(entry);
            return context;
        }

        int start = Math.max(0, userRank - 1);
        int end = Math.min(allRankings.size(), userRank + 2);

        for (int i = start; i < end; i++) {
            Object[] row = allRankings.get(i);
            User user = (User) row[0];
            Long points = (Long) row[1];
            Map<String, Object> entry = new HashMap<>();
            entry.put("id", user.getId());
            entry.put("name", user.getName());
            entry.put("avatar", user.getAvatar());
            entry.put("department", user.getDepartment());
            entry.put("points", points);
            entry.put("rank", i + 1);
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

        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", userId);
        stats.put("totalPoints", totalPoints != null ? totalPoints : 0L);
        stats.put("feedCount", 0); // TODO: FeedRepository에서 조회
        stats.put("commentCount", 0); // TODO: CommentRepository에서 조회
        return stats;
    }

    @Transactional
    public void updateAvatar(User user, String avatarUrl) {
        user.setAvatar(avatarUrl);
        userRepository.save(user);
    }
}
