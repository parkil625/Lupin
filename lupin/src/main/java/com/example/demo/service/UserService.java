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
import com.example.demo.repository.UserPenaltyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate; // [필수] 이 import가 빠져서 에러가 났습니다.
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import com.example.demo.util.RedisKeyUtils; // [추가]
import org.springframework.data.redis.core.RedisTemplate; // [추가]
import org.springframework.data.redis.core.ZSetOperations; // [추가]

import java.util.Map; // [추가]
import java.util.Set; // [추가]
import java.util.function.Function; // [추가]
import java.util.stream.Collectors; // [추가]

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PointService pointService;
    private final UserPenaltyRepository userPenaltyRepository;
    private final FeedRepository feedRepository;
    private final CommentRepository commentRepository;
    private final PointLogRepository pointLogRepository;
    private final RedisTemplate<String, String> redisTemplate; // [추가]

    public User getUserInfo(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void updateProfile(User user, String name, Double height, Double weight, LocalDate birthDate, String gender) {
        user.updateProfile(name, height, weight, birthDate, gender);
        userRepository.save(user);
    }

    public List<UserRankingResponse> getTopUsersByPoints(int limit) {
        String key = RedisKeyUtils.rankingKey(YearMonth.now().toString());

        // [Fix] Redis 데이터가 없으면 DB 동기화 실행 (Lazy Loading)
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            refreshMonthlyRanking(key);
        }
        
        // 1. Redis에서 상위 랭커 조회 (점수 포함)
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. User 상세 정보 조회를 위해 ID 리스트 추출
        List<Long> userIds = tuples.stream()
                .map(tuple -> Long.parseLong(tuple.getValue()))
                .toList();

        // 3. DB에서 한 번에 User 정보 조회 (Map으로 변환하여 매핑 최적화)
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        // 4. Redis 순서대로 응답 조합
        List<UserRankingResponse> rankings = new ArrayList<>();
        int rank = 1;
        
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long userId = Long.parseLong(tuple.getValue());
            User user = userMap.get(userId);
            
            if (user != null) {
                long points = tuple.getScore().longValue();
                // 포인트가 음수일 경우 0으로 표기
                long displayPoints = Math.max(0, points);
                rankings.add(UserRankingResponse.of(user, displayPoints, rank));
            }
            rank++;
        }
        
        return rankings;
    }

    public List<UserRankingResponse> getUserRankingContext(Long userId) {
        String key = RedisKeyUtils.rankingKey(YearMonth.now().toString());

        // [Fix] Redis 데이터가 없으면 DB 동기화 실행
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            refreshMonthlyRanking(key);
        }

        // 1. 내 등수 확인 (0부터 시작하므로 +1 필요)
        Long myRankIndex = redisTemplate.opsForZSet().reverseRank(key, String.valueOf(userId));
        if (myRankIndex == null) {
            return new ArrayList<>(); // 랭킹에 없는 경우
        }

        // 2. 앞뒤 유저 범위 계산 (예: 1등이면 0~1, 중간이면 -1~+1)
        long start = Math.max(0, myRankIndex - 1);
        long end = myRankIndex + 1;

        // 3. Redis 범위 조회
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores(key, start, end);

        if (tuples == null || tuples.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. DB 정보 매핑 및 결과 생성 (위와 동일한 로직 재사용 가능)
        List<Long> userIds = tuples.stream()
                .map(tuple -> Long.parseLong(tuple.getValue()))
                .toList();
                
        Map<Long, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<UserRankingResponse> responses = new ArrayList<>();
        // start 등수부터 시작
        int currentRank = (int) start + 1; 

        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long uid = Long.parseLong(tuple.getValue());
            User user = userMap.get(uid);
            if (user != null) {
                long points = tuple.getScore().longValue();
                responses.add(UserRankingResponse.of(user, Math.max(0, points), currentRank));
            }
            currentRank++;
        }
        return responses;
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
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime end = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        Double avg = pointLogRepository.getAveragePointsPerUser(start, end);
        long result = avg != null ? Math.round(avg) : 0L;
        
        log.info(">>> [UserService] Average points for {}: {}", currentMonth, result);
        return result;
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
     * [Fix] Redis 캐시 미스 시 DB 데이터를 기반으로 랭킹 복구
     */
    private void refreshMonthlyRanking(String key) {
        log.info(">>> [Ranking] Cache miss. Recovering ranking data from DB...");
        
        YearMonth currentMonth = YearMonth.now();
        LocalDateTime start = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime end = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        // DB에서 이번 달 유저별 합계 조회 (Repository에 추가한 쿼리 사용)
        List<Object[]> summaries = pointLogRepository.sumPointsPerUser(start, end);

        if (summaries.isEmpty()) {
            return;
        }

        // Redis ZSet에 일괄 삽입
        for (Object[] row : summaries) {
            Long userId = (Long) row[0];
            Long totalPoints = ((Number) row[1]).longValue();
            
            if (totalPoints > 0) {
                redisTemplate.opsForZSet().add(key, String.valueOf(userId), totalPoints);
            }
        }
        
        // 키 만료 시간 설정 (약 40일)
        redisTemplate.expire(key, java.time.Duration.ofDays(40));
        
        log.info(">>> [Ranking] Recovered {} users into Redis.", summaries.size());
    }
}