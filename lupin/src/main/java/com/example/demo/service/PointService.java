package com.example.demo.service;

import com.example.demo.config.properties.FeedProperties;
import com.example.demo.domain.entity.PointLog;
import com.example.demo.domain.entity.User;
import com.example.demo.event.PointChangedEvent;
import com.example.demo.repository.PointLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PointService {

    private final PointLogRepository pointLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final RedisTemplate<String, String> redisTemplate;
    private final FeedProperties feedProperties;

    public long getTotalPoints(User user) {
        return user.getTotalPoints();
    }

    public long getMonthlyPoints(User user, YearMonth yearMonth) {
        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        long sum = pointLogRepository.sumPointsByUserIdAndMonth(
                user.getId(),
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.atEndOfMonth().atTime(23, 59, 59)
        );
        return Math.max(sum, 0);
    }

    @Transactional
    public void addPoints(User user, long amount) {
        savePointLog(user, amount);

        updateRedisRankingAfterCommit(user, amount);

        eventPublisher.publishEvent(PointChangedEvent.add(user.getId(), amount));
    }

    @Transactional
    public void deductPoints(User user, long amount) {
        savePointLog(user, -amount);
        eventPublisher.publishEvent(PointChangedEvent.deduct(user.getId(), amount));
    }

    @Transactional
    public void recoverFeedPoints(User user, long points, LocalDateTime feedCreatedAt) {
        int recoveryDays = feedProperties.getPointRecoveryDays();
        LocalDateTime deadline = LocalDateTime.now().minusDays(recoveryDays);
        if (feedCreatedAt.isBefore(deadline)) {
            return;
        }

        cancelPoints(user, points);
    }

    @Transactional
    public void cancelPoints(User user, long amount) {
        // 1. DB 장부에서 빼기
        savePointLog(user, -amount);

        // 2. Redis 랭킹에서도 빼기 (커밋 후 반영)
        updateRedisRankingAfterCommit(user, -amount);

        // 이벤트는 차감과 동일하게 발생 (필요하다면 타입을 CANCEL로 구분 가능)
        eventPublisher.publishEvent(PointChangedEvent.deduct(user.getId(), amount));
    }

    /**
     * 피드 포인트 조정 (수정 시 기존 포인트 회수 후 새 포인트 부여)
     * 포인트 정책은 PointService가 담당 (SRP)
     */
    @Transactional
    public void adjustFeedPoints(User user, long oldPoints, long newPoints) {
        if (oldPoints > 0) {
            // deductPoints(사용) 대신 cancelPoints(정정)를 써야 해요!
            cancelPoints(user, oldPoints);
        }
        if (newPoints > 0) {
            addPoints(user, newPoints);
        }
    }

    private void savePointLog(User user, long points) {
        PointLog pointLog = PointLog.builder()
                .user(user)
                .points(points)
                .build();
        pointLogRepository.save(pointLog);
    }

    /**
     * 트랜잭션 커밋 후에만 Redis 랭킹 업데이트 (데이터 정합성 보장)
     */
    private void updateRedisRankingAfterCommit(User user, long amount) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    String key = "ranking:monthly:" + YearMonth.now().toString();
                    redisTemplate.opsForZSet().incrementScore(key, String.valueOf(user.getId()), amount);
                }
            });
        } else {
            // 트랜잭션이 없는 경우 즉시 실행
            String key = "ranking:monthly:" + YearMonth.now().toString();
            redisTemplate.opsForZSet().incrementScore(key, String.valueOf(user.getId()), amount);
        }
    }
}
