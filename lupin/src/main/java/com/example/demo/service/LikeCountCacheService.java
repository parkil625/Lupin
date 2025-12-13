package com.example.demo.service;

import com.example.demo.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 좋아요 카운트 Redis 캐싱 서비스
 * - Hot Write 문제 해결: DB 대신 Redis에서 카운트 관리
 * - Write-Behind 패턴: Redis 업데이트 후 비동기 DB 동기화
 * - Redis Pipeline: 네트워크 RTT 최소화
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikeCountCacheService {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 좋아요 카운트 증가 (Redis Pipeline으로 최적화)
     * - increment + sadd를 한 번의 네트워크 호출로 처리
     * @param feedId 피드 ID
     * @return 증가 후 카운트
     */
    public Long incrementLikeCount(Long feedId) {
        String countKey = RedisKeyUtils.feedLikeCount(feedId);
        String dirtySetKey = RedisKeyUtils.feedLikeDirtySet();
        String feedIdStr = feedId.toString();

        // Redis Pipeline: 2번의 네트워크 호출 → 1번으로 최적화
        List<Object> results = redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.opsForValue().increment(countKey);
                operations.opsForSet().add(dirtySetKey, feedIdStr);
                return null;
            }
        });

        Long count = results.get(0) != null ? ((Number) results.get(0)).longValue() : 0L;
        log.debug("Redis like count incremented (pipelined): feedId={}, count={}", feedId, count);
        return count;
    }

    /**
     * 좋아요 카운트 감소 (Lua Script + Pipeline)
     * - Lua Script로 원자적 처리 (음수 방지)
     * - Dirty Set 추가는 Pipeline으로 처리
     * @param feedId 피드 ID
     * @return 감소 후 카운트
     */
    public Long decrementLikeCount(Long feedId) {
        String countKey = RedisKeyUtils.feedLikeCount(feedId);
        String dirtySetKey = RedisKeyUtils.feedLikeDirtySet();
        String feedIdStr = feedId.toString();

        // Lua Script: 감소 후 음수면 0으로 설정 + Dirty Set 추가 (원자적 연산)
        String script =
                "local current = redis.call('decr', KEYS[1]); " +
                "if current < 0 then " +
                "   redis.call('set', KEYS[1], 0); " +
                "   current = 0; " +
                "end; " +
                "redis.call('sadd', KEYS[2], ARGV[1]); " +
                "return current;";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);
        Long count = redisTemplate.execute(
                redisScript,
                List.of(countKey, dirtySetKey),
                feedIdStr
        );

        log.debug("Redis like count decremented: feedId={}, count={}", feedId, count);
        return count != null ? count : 0L;
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
     * 좋아요 카운트 캐시 삭제 (Pipeline 최적화)
     * @param feedId 피드 ID
     */
    public void deleteLikeCount(Long feedId) {
        String countKey = RedisKeyUtils.feedLikeCount(feedId);
        String dirtySetKey = RedisKeyUtils.feedLikeDirtySet();
        String feedIdStr = feedId.toString();

        // Redis Pipeline으로 2개 연산을 1번의 네트워크 호출로 처리
        redisTemplate.executePipelined(new SessionCallback<>() {
            @Override
            @SuppressWarnings("unchecked")
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.delete(countKey);
                operations.opsForSet().remove(dirtySetKey, feedIdStr);
                return null;
            }
        });

        log.debug("Redis like count deleted: feedId={}", feedId);
    }
}
