package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Lua Script 기반 원자적 연산 서비스
 * - 복합 연산의 원자성 보장
 * - Race Condition 완전 제거
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisLuaService {

    private final RedisTemplate<String, String> redisTemplate;

    // ==================== Lua Scripts ====================

    // 재고 차감 (체크 + 차감 원자적)
    private static final String DECREMENT_STOCK_SCRIPT = """
        local stock = tonumber(redis.call('GET', KEYS[1]) or '0')
        if stock > 0 then
            return redis.call('DECR', KEYS[1])
        else
            return -1
        end
        """;

    // 챌린지 참가 (중복 체크 + 추가 + 카운터 증가 원자적)
    private static final String JOIN_CHALLENGE_SCRIPT = """
        local added = redis.call('SADD', KEYS[1], ARGV[1])
        if added == 1 then
            redis.call('INCR', KEYS[2])
            return 1
        else
            return 0
        end
        """;

    // 포인트 차감 (잔액 체크 + 차감 원자적)
    private static final String DEDUCT_POINTS_SCRIPT = """
        local current = tonumber(redis.call('GET', KEYS[1]) or '0')
        local amount = tonumber(ARGV[1])
        if current >= amount then
            redis.call('DECRBY', KEYS[1], amount)
            return current - amount
        else
            return -1
        end
        """;

    // 좋아요 토글 (있으면 삭제, 없으면 추가)
    private static final String TOGGLE_LIKE_SCRIPT = """
        local exists = redis.call('SISMEMBER', KEYS[1], ARGV[1])
        if exists == 1 then
            redis.call('SREM', KEYS[1], ARGV[1])
            redis.call('DECR', KEYS[2])
            return -1
        else
            redis.call('SADD', KEYS[1], ARGV[1])
            redis.call('INCR', KEYS[2])
            return 1
        end
        """;

    // 추첨권 발급 (포인트 체크 + 차감 + 티켓 발급 원자적)
    private static final String ISSUE_TICKET_SCRIPT = """
        local current = tonumber(redis.call('GET', KEYS[1]) or '0')
        if current >= 30 then
            redis.call('DECRBY', KEYS[1], 30)
            redis.call('INCR', KEYS[2])
            return 1
        else
            return 0
        end
        """;

    // Rate Limiting (요청 수 체크)
    private static final String RATE_LIMIT_SCRIPT = """
        local current = tonumber(redis.call('GET', KEYS[1]) or '0')
        local limit = tonumber(ARGV[1])
        local ttl = tonumber(ARGV[2])
        if current >= limit then
            return 0
        else
            redis.call('INCR', KEYS[1])
            if current == 0 then
                redis.call('EXPIRE', KEYS[1], ttl)
            end
            return 1
        end
        """;

    // ==================== 원자적 연산 메서드 ====================

    /**
     * 재고 차감 (원자적)
     * @return 남은 재고, -1이면 재고 부족
     */
    public Long decrementStockAtomic(Long prizeId) {
        String key = "prize:stock:" + prizeId;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(DECREMENT_STOCK_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(script, Collections.singletonList(key));

        if (result != null && result == -1) {
            log.warn("재고 부족: prizeId={}", prizeId);
        }

        return result;
    }

    /**
     * 챌린지 참가 (원자적)
     * @return true: 참가 성공, false: 이미 참가함
     */
    public boolean joinChallengeAtomic(Long challengeId, Long userId) {
        String entriesKey = "challenge:entries:" + challengeId;
        String countKey = "challenge:count:" + challengeId;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(JOIN_CHALLENGE_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
            script,
            List.of(entriesKey, countKey),
            String.valueOf(userId)
        );

        return result != null && result == 1;
    }

    /**
     * 포인트 차감 (원자적)
     * @return 남은 포인트, -1이면 포인트 부족
     */
    public Long deductPointsAtomic(Long userId, Long amount) {
        String key = "user:points:" + userId;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(DEDUCT_POINTS_SCRIPT);
        script.setResultType(Long.class);

        return redisTemplate.execute(
            script,
            Collections.singletonList(key),
            String.valueOf(amount)
        );
    }

    /**
     * 좋아요 토글 (원자적)
     * @return 1: 좋아요 추가, -1: 좋아요 취소
     */
    public Long toggleLikeAtomic(String likeSetKey, String countKey, Long userId) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(TOGGLE_LIKE_SCRIPT);
        script.setResultType(Long.class);

        return redisTemplate.execute(
            script,
            List.of(likeSetKey, countKey),
            String.valueOf(userId)
        );
    }

    /**
     * 피드 좋아요 토글
     */
    public Long toggleFeedLike(Long feedId, Long userId) {
        String likeSetKey = "feed:like:set:" + feedId;
        String countKey = "feed:likes:" + feedId;
        return toggleLikeAtomic(likeSetKey, countKey, userId);
    }

    /**
     * 댓글 좋아요 토글
     */
    public Long toggleCommentLike(Long commentId, Long userId) {
        String likeSetKey = "comment:like:set:" + commentId;
        String countKey = "comment:likes:" + commentId;
        return toggleLikeAtomic(likeSetKey, countKey, userId);
    }

    /**
     * 추첨권 발급 (원자적)
     * @return true: 발급 성공, false: 포인트 부족
     */
    public boolean issueTicketAtomic(Long userId) {
        String pointsKey = "user:points:" + userId;
        String ticketsKey = "user:tickets:" + userId;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(ISSUE_TICKET_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
            script,
            List.of(pointsKey, ticketsKey)
        );

        return result != null && result == 1;
    }

    /**
     * Rate Limiting 체크
     * @return true: 요청 허용, false: 제한 초과
     */
    public boolean checkRateLimit(String key, int limit, int windowSeconds) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(RATE_LIMIT_SCRIPT);
        script.setResultType(Long.class);

        Long result = redisTemplate.execute(
            script,
            Collections.singletonList(key),
            String.valueOf(limit),
            String.valueOf(windowSeconds)
        );

        return result != null && result == 1;
    }

    /**
     * API Rate Limiting
     */
    public boolean checkApiRateLimit(Long userId, String api, int limit, int windowSeconds) {
        String key = "ratelimit:" + api + ":" + userId;
        return checkRateLimit(key, limit, windowSeconds);
    }
}
