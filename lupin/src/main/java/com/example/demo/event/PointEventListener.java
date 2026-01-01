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

    @org.springframework.scheduling.annotation.Async // [추가] 비동기 처리
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePointChangedEvent(PointChangedEvent event) {
        try {
            // 1. DB 업데이트
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

            // 2. Redis 랭킹 실시간 업데이트
            String rankingKey = RedisKeyUtils.rankingKey(YearMonth.now().toString());
            String userIdStr = String.valueOf(event.userId());
            
            // 점수 업데이트 (없으면 자동 생성됨)
            Double newScore = redisTemplate.opsForZSet().incrementScore(rankingKey, userIdStr, event.amount());
            
            // [수정] 점수가 0 이하가 되어도 랭킹에서 제거하지 않음 (0점 유저 노출 보장)
            // 오히려 음수가 되면 0점으로 보정하여 저장
            if (newScore != null && newScore < 0) {
                redisTemplate.opsForZSet().add(rankingKey, userIdStr, 0);
                log.debug(">>> [Ranking] User {} score adjusted from {} to 0 (Negative points).", userIdStr, newScore);
            }
            
            // 키 만료 시간 연장 (40일)
            redisTemplate.expire(rankingKey, java.time.Duration.ofDays(40));
            
            log.debug(">>> [Ranking] User {} updated. Delta: {}, NewScore: {}", userIdStr, event.amount(), newScore);

        } catch (Exception e) {
            log.error("Failed to update user points or ranking: {}", event, e);
        }
    }
}
