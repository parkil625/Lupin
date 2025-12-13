package com.example.demo.event;

import com.example.demo.service.LikeCountCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 피드 삭제 이벤트 리스너
 * 트랜잭션 커밋 후 비동기 정리 작업 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedDeletedEventListener {

    private final LikeCountCacheService likeCountCacheService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFeedDeletedEvent(FeedDeletedEvent event) {
        try {
            // Redis 캐시 정리
            likeCountCacheService.deleteLikeCount(event.feedId());
            log.debug("Feed deletion cleanup completed: feedId={}", event.feedId());
        } catch (Exception e) {
            // 캐시 삭제 실패는 치명적이지 않으므로 로그만 남김
            log.warn("Failed to cleanup cache after feed deletion: feedId={}", event.feedId(), e);
        }
    }
}
