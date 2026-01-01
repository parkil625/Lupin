package com.example.demo.event;

import com.example.demo.domain.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.RedisKeyUtils; // [추가]
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate; // [추가]
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.YearMonth; // [추가]

/**
 * 포인트 이벤트 리스너 - 트랜잭션 커밋 후 User.totalPoints 업데이트
 *
 * PointLog가 저장된 후 User.totalPoints를 동기화하여
 * 반정규화 필드의 일관성을 유지합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointEventListener {

    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate; // [추가] RedisTemplate 주입

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePointChangedEvent(PointChangedEvent event) {
        try {
            // 1. DB 업데이트 (기존 로직 유지)
            // 비관적 락으로 동시성 제어 - 동시 포인트 변경 시 lost update 방지
            User user = userRepository.findByIdForUpdate(event.userId())
                    .orElse(null);
            if (user == null) {
                log.warn("User not found for point update: {}", event.userId());
                return;
            }

            if (event.amount() > 0) {
                user.addPoints(event.amount());
            } else if (event.amount() < 0) {
                user.deductPoints(-event.amount());
            }

            userRepository.save(user);
            log.debug("User points updated: userId={}, delta={}, newTotal={}",
                    event.userId(), event.amount(), user.getTotalPoints());

            // 2. [추가] Redis 랭킹 점수 업데이트 (ZSet)
            // 이번 달 랭킹 키 생성 (예: ranking:monthly:2024-01)
            String rankingKey = RedisKeyUtils.rankingKey(YearMonth.now().toString());
            
            // ZINCRBY: 해당 유저의 점수를 delta만큼 증가 (음수면 감소)
            redisTemplate.opsForZSet().incrementScore(rankingKey, String.valueOf(event.userId()), event.amount());
            
            log.debug("Redis ranking updated: key={}, userId={}, delta={}", rankingKey, event.userId(), event.amount());

        } catch (Exception e) {
            log.error("Failed to update user points: {}", event, e);
        }
    }
}
