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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PointLogRepository pointLogRepository;
    private final FeedRepository feedRepository;
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public User getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void updateProfile(User user, String name, Double height, Double weight) {
        user.updateProfile(name, height, weight);
    }

    public List<UserRankingResponse> getTopUsersByPoints(int limit) {
        String key = "ranking:monthly:" + YearMonth.now().toString();

        // 1. Redis에서 점수가 높은 순서대로 (Reverse) 가져와요.
        // TypedTuple은 "유저ID"와 "점수"를 같이 담고 있는 바구니예요.
        Set<ZSetOperations.TypedTuple<String>> topRankings =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, limit - 1);

        if (topRankings == null || topRankings.isEmpty()) {
            return new ArrayList<>();
        }

        // [N+1 문제 해결] ID 목록 추출 후 Bulk 조회
        List<Long> userIds = topRankings.stream()
                .map(tuple -> Long.valueOf(tuple.getValue()))
                .toList();

        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<UserRankingResponse> responses = new ArrayList<>();
        int rank = 1;

        // 2. 랭킹 순서대로 조립
        for (ZSetOperations.TypedTuple<String> tuple : topRankings) {
            Long userId = Long.valueOf(tuple.getValue());
            long points = tuple.getScore().longValue();

            User user = userMap.get(userId);

            if (user != null) {
                responses.add(UserRankingResponse.of(user, points, rank++));
            }
        }

        return responses;
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
