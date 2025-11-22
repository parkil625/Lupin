package com.example.demo.service;

import com.example.demo.domain.entity.QUser;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 사용자 Query 서비스 (읽기 전용)
 * CQRS 패턴 - 데이터 조회 작업 담당
 * 랭킹 조회 등 복잡한 쿼리 최적화
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final JPAQueryFactory queryFactory;
    private final UserRepository userRepository;
    private final FeedRepository feedRepository;

    private final QUser user = QUser.user;

    /**
     * 전체 랭킹 조회 (상위 N명)
     */
    public List<Map<String, Object>> getTopRankings(int limit) {
        List<User> topUsers = queryFactory
                .selectFrom(user)
                .orderBy(
                        user.monthlyPoints.desc(),
                        user.monthlyLikes.desc(),
                        user.realName.asc()
                )
                .limit(limit)
                .fetch();

        return topUsers.stream()
                .map(u -> {
                    int rank = topUsers.indexOf(u) + 1;
                    return createUserRankingMap(u, rank);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자 주변 랭킹 조회 (본인 + 앞뒤 1명)
     */
    public List<Map<String, Object>> getUserRankingContext(Long userId) {
        findUserById(userId); // 유효성 검사

        // 전체 사용자를 정렬하여 조회
        List<User> allUsers = queryFactory
                .selectFrom(user)
                .orderBy(
                        user.monthlyPoints.desc(),
                        user.monthlyLikes.desc(),
                        user.realName.asc()
                )
                .fetch();

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

        if (currentUserIndex > 0) {
            contextUsers.add(allUsers.get(currentUserIndex - 1));
        }
        contextUsers.add(allUsers.get(currentUserIndex));
        if (currentUserIndex < allUsers.size() - 1) {
            contextUsers.add(allUsers.get(currentUserIndex + 1));
        }

        return contextUsers.stream()
                .map(u -> {
                    int rank = allUsers.indexOf(u) + 1;
                    return createUserRankingMap(u, rank);
                })
                .collect(Collectors.toList());
    }

    /**
     * 사용자 정보 조회
     */
    public Map<String, Object> getUserInfo(Long userId) {
        User userEntity = findUserById(userId);

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", userEntity.getId());
        userMap.put("userId", userEntity.getUserId());
        userMap.put("email", userEntity.getEmail());
        userMap.put("realName", userEntity.getRealName());
        userMap.put("role", userEntity.getRole());
        userMap.put("height", userEntity.getHeight());
        userMap.put("weight", userEntity.getWeight());
        userMap.put("gender", userEntity.getGender());
        userMap.put("birthDate", userEntity.getBirthDate());
        userMap.put("currentPoints", userEntity.getCurrentPoints());
        userMap.put("monthlyPoints", userEntity.getMonthlyPoints());
        userMap.put("monthlyLikes", userEntity.getMonthlyLikes());
        userMap.put("department", userEntity.getDepartment());

        return userMap;
    }

    /**
     * 사용자 존재 여부 확인
     */
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }

    // === Helper Methods ===

    private Map<String, Object> createUserRankingMap(User u, int rank) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", u.getId());
        userMap.put("rank", rank);
        userMap.put("name", u.getRealName());
        userMap.put("points", u.getMonthlyPoints());
        userMap.put("likes", u.getMonthlyLikes());
        userMap.put("department", u.getDepartment());

        // 활동일수 계산
        Integer activeDays = feedRepository.countUserActiveDaysInCurrentMonth(u.getId());
        userMap.put("activeDays", activeDays);

        return userMap;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
