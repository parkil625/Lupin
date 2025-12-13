package com.example.demo.scheduler;

import com.example.demo.repository.FeedRepository;
import com.example.demo.service.LikeCountCacheService;
import com.example.demo.util.RedisKeyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 좋아요 카운트 Redis → DB 동기화 스케줄러
 * - Write-Behind 패턴: Redis의 변경사항을 주기적으로 DB에 반영
 * - Hot Write 문제 해결: DB 락 경합 최소화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LikeCountSyncScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final FeedRepository feedRepository;
    private final LikeCountCacheService likeCountCacheService;

    /**
     * 10초마다 Dirty Set의 피드들을 DB와 동기화
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void syncLikeCountsToDb() {
        Set<String> dirtyFeedIds = redisTemplate.opsForSet()
                .members(RedisKeyUtils.feedLikeDirtySet());

        if (dirtyFeedIds == null || dirtyFeedIds.isEmpty()) {
            return;
        }

        log.debug("Syncing {} dirty feed like counts to DB", dirtyFeedIds.size());

        int successCount = 0;
        int failCount = 0;

        for (String feedIdStr : dirtyFeedIds) {
            try {
                Long feedId = Long.parseLong(feedIdStr);
                Integer cachedCount = likeCountCacheService.getLikeCount(feedId);

                if (cachedCount != null) {
                    feedRepository.updateLikeCount(feedId, cachedCount);
                    successCount++;
                }

                // Dirty Set에서 제거
                redisTemplate.opsForSet().remove(RedisKeyUtils.feedLikeDirtySet(), feedIdStr);

            } catch (NumberFormatException e) {
                log.warn("Invalid feed ID in dirty set: {}", feedIdStr);
                redisTemplate.opsForSet().remove(RedisKeyUtils.feedLikeDirtySet(), feedIdStr);
                failCount++;
            } catch (Exception e) {
                log.error("Failed to sync like count for feedId={}", feedIdStr, e);
                failCount++;
            }
        }

        if (successCount > 0 || failCount > 0) {
            log.info("Like count sync completed: success={}, fail={}", successCount, failCount);
        }
    }
}
