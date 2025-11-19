package com.example.demo.service;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.response.UserProfileResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 사용자 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 프로필 조회
     */
    public UserProfileResponse getUserProfile(Long userId) {
        User user = findUserById(userId);

        Long feedCount = feedRepository.countUserFeeds(userId);
        Long totalActivityMinutes = feedRepository.sumUserActivityDuration(userId);

        return UserProfileResponse.from(
                user,
                feedCount.intValue(),
                totalActivityMinutes != null ? totalActivityMinutes.intValue() : 0
        );
    }

    /**
     * 포인트 적립
     */
    @Transactional
    public void addPoints(Long userId, Long amount, String reason, String refId) {
        User user = findUserById(userId);

        // 포인트 적립
        user.addPoints(amount);

        // 포인트 로그 생성
        PointLog pointLog = PointLog.builder()
                .amount(amount)
                .reason(reason)
                .refId(refId)
                .build();
        pointLog.setUser(user);

        log.info("포인트 적립 완료 - userId: {}, amount: {}, reason: {}", userId, amount, reason);
    }

    /**
     * 포인트 차감
     */
    @Transactional
    public void deductPoints(Long userId, Long amount, String reason) {
        User user = findUserById(userId);

        try {
            user.usePoints(amount);

            // 포인트 로그 생성 (음수로 기록)
            PointLog pointLog = PointLog.builder()
                    .amount(-amount)
                    .reason(reason)
                    .build();
            pointLog.setUser(user);

            log.info("포인트 차감 완료 - userId: {}, amount: {}, reason: {}", userId, amount, reason);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
        }
    }

    /**
     * 이메일로 사용자 조회
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * ID로 사용자 조회 (내부 메서드)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 이메일 중복 확인
     */
    public boolean isEmailExists(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 상위 포인트 사용자 조회 (랭킹)
     */
    public List<Map<String, Object>> getTopUsersByPoints(int limit) {
        List<User> allUsers = userRepository.findAll();

        // 각 사용자의 이번 달 좋아요 수를 미리 계산
        Map<Long, Integer> userLikesMap = allUsers.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        user -> feedRepository.countUserTotalLikesInCurrentMonth(user.getId())
                ));

        // 다단계 정렬: 1) 포인트 내림차순 2) 이번 달 좋아요 수 내림차순 3) 이름 오름차순
        List<User> topUsers = allUsers.stream()
                .sorted((u1, u2) -> {
                    // 1차: 포인트 비교 (내림차순)
                    int pointsCompare = Long.compare(u2.getCurrentPoints(), u1.getCurrentPoints());
                    if (pointsCompare != 0) return pointsCompare;

                    // 2차: 이번 달 좋아요 수 비교 (내림차순)
                    int likesCompare = Integer.compare(
                            userLikesMap.getOrDefault(u2.getId(), 0),
                            userLikesMap.getOrDefault(u1.getId(), 0)
                    );
                    if (likesCompare != 0) return likesCompare;

                    // 3차: 이름 비교 (오름차순)
                    return u1.getRealName().compareTo(u2.getRealName());
                })
                .limit(limit)
                .collect(Collectors.toList());

        return topUsers.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("name", user.getRealName());
                    userMap.put("points", user.getCurrentPoints());
                    userMap.put("profileImage", user.getProfileImage());
                    userMap.put("department", user.getDepartment());
                    // 실제 데이터 계산
                    Integer activeDays = feedRepository.countUserActiveDaysInCurrentMonth(user.getId());
                    userMap.put("activeDays", activeDays);
                    userMap.put("avgScore", user.getCurrentPoints());
                    return userMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자 주변 랭킹 조회 (본인 + 앞뒤 1명)
     */
    public List<Map<String, Object>> getUserRankingContext(Long userId) {
        User currentUser = findUserById(userId);

        // 모든 사용자를 포인트 순으로 정렬
        List<User> allUsers = userRepository.findAll().stream()
                .sorted((u1, u2) -> Long.compare(u2.getCurrentPoints(), u1.getCurrentPoints()))
                .collect(Collectors.toList());

        // 현재 사용자의 인덱스 찾기
        int currentUserIndex = -1;
        for (int i = 0; i < allUsers.size(); i++) {
            if (allUsers.get(i).getId().equals(userId)) {
                currentUserIndex = i;
                break;
            }
        }

        if (currentUserIndex == -1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 앞뒤 1명 포함한 리스트 생성
        List<User> contextUsers = new ArrayList<>();

        // 앞 사람 추가 (있으면)
        if (currentUserIndex > 0) {
            contextUsers.add(allUsers.get(currentUserIndex - 1));
        }

        // 본인 추가
        contextUsers.add(allUsers.get(currentUserIndex));

        // 뒷 사람 추가 (있으면)
        if (currentUserIndex < allUsers.size() - 1) {
            contextUsers.add(allUsers.get(currentUserIndex + 1));
        }

        // Map으로 변환
        return contextUsers.stream()
                .map(user -> {
                    int rank = allUsers.indexOf(user) + 1;
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("rank", rank);
                    userMap.put("name", user.getRealName());
                    userMap.put("points", user.getCurrentPoints());
                    userMap.put("profileImage", user.getProfileImage());
                    userMap.put("department", user.getDepartment());
                    // 실제 데이터 계산
                    Integer activeDays = feedRepository.countUserActiveDaysInCurrentMonth(user.getId());
                    userMap.put("activeDays", activeDays);
                    userMap.put("avgScore", user.getCurrentPoints());
                    return userMap;
                })
                .collect(Collectors.toList());
    }

    /**
     * 전체 통계 조회
     */
    public Map<String, Object> getStatistics() {
        List<User> allUsers = userRepository.findAll();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", allUsers.size());

        // 이번 달 활동한 사용자 수 (포인트가 0보다 큰 사용자)
        long activeUsers = allUsers.stream()
                .filter(user -> user.getCurrentPoints() > 0)
                .count();
        stats.put("activeUsersThisMonth", activeUsers);

        // 평균 포인트
        double avgPoints = allUsers.stream()
                .mapToLong(User::getCurrentPoints)
                .average()
                .orElse(0.0);
        stats.put("averagePoints", (int) Math.round(avgPoints));

        return stats;
    }
}
