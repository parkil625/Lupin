package com.example.demo.service;

import com.example.demo.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 좋아요 카운트 Redis 캐싱 서비스
 * - Hot Write 문제 해결: DB 대신 Redis에서 카운트 관리
 * - Write-Behind 패턴: Redis 업데이트 후 비동기 DB 동기화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeCountCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 좋아요 카운트 증가 (Redis)
     * @param feedId 피드 ID
     * @return 증가 후 카운트
     */
    public Long incrementLikeCount(Long feedId) {
        String key = RedisKeyUtils.feedLikeCount(feedId);
        Long count = redisTemplate.opsForValue().increment(key);

        // Dirty Set에 추가 (DB 동기화 대상)
        redisTemplate.opsForSet().add(RedisKeyUtils.feedLikeDirtySet(), feedId.toString());

        log.debug("Redis like count incremented: feedId={}, count={}", feedId, count);
        return count;
    }

    /**
     * 좋아요 카운트 감소 (Redis)
     * - Lua Script로 원자적 처리 (Check-Then-Act 경합 방지)
     * @param feedId 피드 ID
     * @return 감소 후 카운트
     */
    public Long decrementLikeCount(Long feedId) {
        String key = RedisKeyUtils.feedLikeCount(feedId);

        // Lua Script: 감소 후 음수면 0으로 설정 (원자적 연산)
        String script =
                "local current = redis.call('decr', KEYS[1]); " +
                "if current < 0 then " +
                "   redis.call('set', KEYS[1], 0); " +
                "   return 0; " +
                "end; " +
                "return current;";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long count = redisTemplate.execute(redisScript, Collections.singletonList(key));

        // Dirty Set에 추가
        redisTemplate.opsForSet().add(RedisKeyUtils.feedLikeDirtySet(), feedId.toString());

        log.debug("Redis like count decremented: feedId={}, count={}", feedId, count);
        return count;
    }

    /**
     * 좋아요 카운트 조회 (Redis 우선, 없으면 null)
     * @param feedId 피드 ID
     * @return 캐시된 카운트 (없으면 null)
     */
    public Integer getLikeCount(Long feedId) {
        String key = RedisKeyUtils.feedLikeCount(feedId);
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Invalid like count in Redis: feedId={}, value={}", feedId, value);
            return null;
        }
    }

    /**
     * 좋아요 카운트 초기화 (DB 값으로 캐시 설정)
     * @param feedId 피드 ID
     * @param count DB에서 조회한 카운트
     */
    public void initializeLikeCount(Long feedId, int count) {
        String key = RedisKeyUtils.feedLikeCount(feedId);
        redisTemplate.opsForValue().set(key, String.valueOf(count));
        log.debug("Redis like count initialized: feedId={}, count={}", feedId, count);
    }

    /**
     * 좋아요 카운트 캐시 삭제
     * @param feedId 피드 ID
     */
    public void deleteLikeCount(Long feedId) {
        String key = RedisKeyUtils.feedLikeCount(feedId);
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove(RedisKeyUtils.feedLikeDirtySet(), feedId.toString());
        log.debug("Redis like count deleted: feedId={}", feedId);
    }
}
