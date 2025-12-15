package com.example.demo.service;

import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.YearMonth;

/**
 * 포인트 증감 및 로그 기록, 랭킹 업데이트를 담당하는 저수준 서비스
 * - PointService와 PointEventListener의 공통 로직 추출
 * - 순환 참조 문제 해결
 */
@Component
@RequiredArgsConstructor
public class PointManager {

    private final UserRepository userRepository;
    private final PointLogRepository pointLogRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Transactional
    public void addPoints(User user, long amount) {
        user.addPoints(amount);
        userRepository.save(user);
        savePointLog(user, amount);
        updateRedisRankingAfterCommit(user, amount);
    }

    @Transactional
    public void cancelPoints(User user, long amount) {
        user.deductPoints(amount);
        userRepository.save(user);
        savePointLog(user, -amount);
        updateRedisRankingAfterCommit(user, -amount);
    }

    private void savePointLog(User user, long points) {
        PointLog pointLog = PointLog.builder()
                .user(user)
                .points(points)
                .build();
        pointLogRepository.save(pointLog);
    }

    private void updateRedisRankingAfterCommit(User user, long amount) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    updateRedisRanking(user.getId(), amount);
                }
            });
        } else {
            updateRedisRanking(user.getId(), amount);
        }
    }

    private void updateRedisRanking(Long userId, long amount) {
        String key = "ranking:monthly:" + YearMonth.now().toString();
        redisTemplate.opsForZSet().incrementScore(key, String.valueOf(userId), amount);
    }
}
