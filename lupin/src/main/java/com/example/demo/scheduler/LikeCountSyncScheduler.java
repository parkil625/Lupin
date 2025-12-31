package com.example.demo.scheduler;

import com.example.demo.service.LikeCountCacheService;
import com.example.demo.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 좋아요 카운트 Redis → DB 동기화 스케줄러
 * - Write-Behind 패턴: Redis의 변경사항을 주기적으로 DB에 반영
 * - Hot Write 문제 해결: DB 락 경합 최소화
 * - JDBC Batch Update: N번 쿼리 → 1번 배치 쿼리로 최적화
 */
import org.springframework.context.annotation.Profile;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test") // [수정됨] 테스트 환경에서는 스케줄러 실행 방지
public class LikeCountSyncScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final LikeCountCacheService likeCountCacheService;

    private static final int BATCH_SIZE = 100;

    /**
     * 10초마다 Dirty Set의 피드들을 DB와 동기화
     * JDBC Batch Update로 N번 쿼리를 1번으로 최적화
     */
    @Scheduled(fixedRate = 10000)
    // [수정] Redis 조회 등 DB와 무관한 작업까지 트랜잭션에 묶여 락(Lock)을 유발하므로 @Transactional 제거
    public void syncLikeCountsToDb() {
        Set<String> dirtyFeedIds = redisTemplate.opsForSet()
                .members(RedisKeyUtils.feedLikeDirtySet());

        if (dirtyFeedIds == null || dirtyFeedIds.isEmpty()) {
            return;
        }

        log.debug("Syncing {} dirty feed like counts to DB", dirtyFeedIds.size());

        // 배치 업데이트용 데이터 수집
        List<Object[]> batchArgs = new ArrayList<>();
        List<String> processedIds = new ArrayList<>();

        for (String feedIdStr : dirtyFeedIds) {
            try {
                Long feedId = Long.parseLong(feedIdStr);
                Integer cachedCount = likeCountCacheService.getLikeCount(feedId);

                if (cachedCount != null) {
                    batchArgs.add(new Object[]{cachedCount, feedId});
                    processedIds.add(feedIdStr);
                }

            } catch (NumberFormatException e) {
                log.warn("Invalid feed ID in dirty set: {}", feedIdStr);
                processedIds.add(feedIdStr); // 잘못된 ID도 제거 대상
            }
        }

        // JDBC Batch Update 실행 (N번 쿼리 → 1번 배치)
        if (!batchArgs.isEmpty()) {
            int[] updateCounts = jdbcTemplate.batchUpdate(
                    "UPDATE feeds SET like_count = ? WHERE id = ?",
                    batchArgs
            );

            int successCount = 0;
            for (int count : updateCounts) {
                if (count > 0) successCount++;
            }

            log.info("Like count batch sync completed: total={}, success={}", batchArgs.size(), successCount);
        }

        // Dirty Set에서 처리된 ID들 일괄 제거
        if (!processedIds.isEmpty()) {
            redisTemplate.opsForSet().remove(
                    RedisKeyUtils.feedLikeDirtySet(),
                    processedIds.toArray()
            );
        }
    }
}
