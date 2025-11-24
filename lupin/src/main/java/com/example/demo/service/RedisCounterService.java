package com.example.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 고성능 카운터 및 캐시 서비스
 * - Atomic 연산으로 동시성 문제 해결
 * - DB 부하 최소화
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCounterService {

    private final RedisTemplate<String, String> redisTemplate;

    // ==================== Key Prefixes ====================
    private static final String FEED_LIKES = "feed:likes:";
    private static final String FEED_COMMENTS = "feed:comments:";
    private static final String COMMENT_LIKES = "comment:likes:";
    private static final String COMMENT_REPLIES = "comment:replies:";
    private static final String USER_POINTS = "user:points:";
    private static final String USER_MONTHLY_POINTS = "user:monthly:";
    private static final String USER_MONTHLY_LIKES = "user:likes:";
    private static final String PRIZE_STOCK = "prize:stock:";
    private static final String CHALLENGE_ENTRIES = "challenge:entries:";
    private static final String CHALLENGE_COUNT = "challenge:count:";
    private static final String RANKING_POINTS = "ranking:points";
    private static final String RANKING_LIKES = "ranking:likes";

    // ==================== Feed 카운터 ====================

    public Long incrementFeedLikes(Long feedId) {
        return redisTemplate.opsForValue().increment(FEED_LIKES + feedId);
    }

    public Long decrementFeedLikes(Long feedId) {
        Long result = redisTemplate.opsForValue().decrement(FEED_LIKES + feedId);
        if (result != null && result < 0) {
            redisTemplate.opsForValue().set(FEED_LIKES + feedId, "0");
            return 0L;
        }
        return result;
    }

    public Long getFeedLikes(Long feedId) {
        String value = redisTemplate.opsForValue().get(FEED_LIKES + feedId);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public void setFeedLikes(Long feedId, Long count) {
        redisTemplate.opsForValue().set(FEED_LIKES + feedId, String.valueOf(count));
    }

    public Long incrementFeedComments(Long feedId) {
        return redisTemplate.opsForValue().increment(FEED_COMMENTS + feedId);
    }

    public Long decrementFeedComments(Long feedId) {
        Long result = redisTemplate.opsForValue().decrement(FEED_COMMENTS + feedId);
        if (result != null && result < 0) {
            redisTemplate.opsForValue().set(FEED_COMMENTS + feedId, "0");
            return 0L;
        }
        return result;
    }

    public Long getFeedComments(Long feedId) {
        String value = redisTemplate.opsForValue().get(FEED_COMMENTS + feedId);
        return value != null ? Long.parseLong(value) : 0L;
    }

    // ==================== Comment 카운터 ====================

    public Long incrementCommentLikes(Long commentId) {
        return redisTemplate.opsForValue().increment(COMMENT_LIKES + commentId);
    }

    public Long decrementCommentLikes(Long commentId) {
        Long result = redisTemplate.opsForValue().decrement(COMMENT_LIKES + commentId);
        if (result != null && result < 0) {
            redisTemplate.opsForValue().set(COMMENT_LIKES + commentId, "0");
            return 0L;
        }
        return result;
    }

    public Long incrementCommentReplies(Long commentId) {
        return redisTemplate.opsForValue().increment(COMMENT_REPLIES + commentId);
    }

    public Long decrementCommentReplies(Long commentId) {
        Long result = redisTemplate.opsForValue().decrement(COMMENT_REPLIES + commentId);
        if (result != null && result < 0) {
            redisTemplate.opsForValue().set(COMMENT_REPLIES + commentId, "0");
            return 0L;
        }
        return result;
    }

    // ==================== User 포인트 ====================

    public Long incrementUserPoints(Long userId, Long amount) {
        redisTemplate.opsForValue().increment(USER_POINTS + userId, amount);
        return redisTemplate.opsForValue().increment(USER_MONTHLY_POINTS + userId, amount);
    }

    public Long getUserPoints(Long userId) {
        String value = redisTemplate.opsForValue().get(USER_POINTS + userId);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public Long getUserMonthlyPoints(Long userId) {
        String value = redisTemplate.opsForValue().get(USER_MONTHLY_POINTS + userId);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public void setUserPoints(Long userId, Long current, Long monthly) {
        redisTemplate.opsForValue().set(USER_POINTS + userId, String.valueOf(current));
        redisTemplate.opsForValue().set(USER_MONTHLY_POINTS + userId, String.valueOf(monthly));
    }

    public Long deductUserPoints(Long userId, Long amount) {
        Long current = getUserPoints(userId);
        if (current < amount) {
            return -1L; // 포인트 부족
        }
        return redisTemplate.opsForValue().decrement(USER_POINTS + userId, amount);
    }

    public Long incrementUserMonthlyLikes(Long userId) {
        return redisTemplate.opsForValue().increment(USER_MONTHLY_LIKES + userId);
    }

    public Long decrementUserMonthlyLikes(Long userId) {
        Long result = redisTemplate.opsForValue().decrement(USER_MONTHLY_LIKES + userId);
        if (result != null && result < 0) {
            redisTemplate.opsForValue().set(USER_MONTHLY_LIKES + userId, "0");
            return 0L;
        }
        return result;
    }

    // ==================== Prize 재고 ====================

    /**
     * 재고 차감 (Atomic)
     * @return 남은 재고, -1이면 재고 부족
     */
    public Long decrementPrizeStock(Long prizeId) {
        Long remaining = redisTemplate.opsForValue().decrement(PRIZE_STOCK + prizeId);
        if (remaining != null && remaining < 0) {
            // 롤백
            redisTemplate.opsForValue().increment(PRIZE_STOCK + prizeId);
            return -1L;
        }
        return remaining;
    }

    public Long getPrizeStock(Long prizeId) {
        String value = redisTemplate.opsForValue().get(PRIZE_STOCK + prizeId);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public void setPrizeStock(Long prizeId, Long quantity) {
        redisTemplate.opsForValue().set(PRIZE_STOCK + prizeId, String.valueOf(quantity));
    }

    // ==================== Challenge 참가 ====================

    /**
     * 챌린지 참가 (중복 체크 + 카운터 증가 atomic)
     * @return true: 참가 성공, false: 이미 참가함
     */
    public boolean joinChallenge(Long challengeId, Long userId) {
        String key = CHALLENGE_ENTRIES + challengeId;
        Long added = redisTemplate.opsForSet().add(key, String.valueOf(userId));

        if (added != null && added > 0) {
            redisTemplate.opsForValue().increment(CHALLENGE_COUNT + challengeId);
            return true;
        }
        return false;
    }

    public boolean hasJoinedChallenge(Long challengeId, Long userId) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForSet().isMember(CHALLENGE_ENTRIES + challengeId, String.valueOf(userId))
        );
    }

    public Long getChallengeEntryCount(Long challengeId) {
        String value = redisTemplate.opsForValue().get(CHALLENGE_COUNT + challengeId);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public Set<String> getChallengeEntries(Long challengeId) {
        return redisTemplate.opsForSet().members(CHALLENGE_ENTRIES + challengeId);
    }

    // ==================== 랭킹 (Sorted Set) ====================

    public void updatePointsRanking(Long userId, Long points) {
        redisTemplate.opsForZSet().add(RANKING_POINTS, String.valueOf(userId), points);
    }

    public void updateLikesRanking(Long userId, Long likes) {
        redisTemplate.opsForZSet().add(RANKING_LIKES, String.valueOf(userId), likes);
    }

    /**
     * 포인트 랭킹 상위 N명
     */
    public Set<ZSetOperations.TypedTuple<String>> getTopPointsRanking(int top) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_POINTS, 0, top - 1);
    }

    /**
     * 좋아요 랭킹 상위 N명
     */
    public Set<ZSetOperations.TypedTuple<String>> getTopLikesRanking(int top) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(RANKING_LIKES, 0, top - 1);
    }

    /**
     * 사용자 랭킹 순위 조회
     */
    public Long getUserPointsRank(Long userId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_POINTS, String.valueOf(userId));
        return rank != null ? rank + 1 : null;
    }

    public Long getUserLikesRank(Long userId) {
        Long rank = redisTemplate.opsForZSet().reverseRank(RANKING_LIKES, String.valueOf(userId));
        return rank != null ? rank + 1 : null;
    }

    // ==================== 캐시 유틸리티 ====================

    public void setWithExpiry(String key, String value, Duration duration) {
        redisTemplate.opsForValue().set(key, value, duration);
    }

    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public Boolean setIfAbsent(String key, String value, Duration duration) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, duration);
    }

    // ==================== 월초 리셋 ====================

    public void resetMonthlyData() {
        // 월별 포인트 및 좋아요 키 삭제
        Set<String> monthlyPointsKeys = redisTemplate.keys(USER_MONTHLY_POINTS + "*");
        Set<String> monthlyLikesKeys = redisTemplate.keys(USER_MONTHLY_LIKES + "*");
        Set<String> currentPointsKeys = redisTemplate.keys(USER_POINTS + "*");

        if (monthlyPointsKeys != null && !monthlyPointsKeys.isEmpty()) {
            redisTemplate.delete(monthlyPointsKeys);
        }
        if (monthlyLikesKeys != null && !monthlyLikesKeys.isEmpty()) {
            redisTemplate.delete(monthlyLikesKeys);
        }
        if (currentPointsKeys != null && !currentPointsKeys.isEmpty()) {
            redisTemplate.delete(currentPointsKeys);
        }

        // 랭킹 리셋
        redisTemplate.delete(RANKING_POINTS);
        redisTemplate.delete(RANKING_LIKES);

        log.info("월별 Redis 데이터 리셋 완료");
    }
}
