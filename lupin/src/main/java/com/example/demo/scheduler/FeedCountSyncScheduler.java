package com.example.demo.scheduler;

import com.example.demo.repository.FeedAdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedCountSyncScheduler {

    private final FeedAdminRepository feedAdminRepository;

    /**
     * 피드의 좋아요/댓글 카운트를 실제 데이터와 동기화
     * 매일 새벽 4시에 실행
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void syncFeedCounts() {
        int likeCountUpdated = feedAdminRepository.syncLikeCounts();
        int commentCountUpdated = feedAdminRepository.syncCommentCounts();
        
        // [추가] 댓글 좋아요 수 동기화 실행
        int commentLikeCountUpdated = feedAdminRepository.syncCommentLikeCounts();

        if (likeCountUpdated > 0 || commentCountUpdated > 0 || commentLikeCountUpdated > 0) {
            log.info("데이터 카운트 동기화 완료 - 피드좋아요: {}개, 피드댓글: {}개, 댓글좋아요: {}개 업데이트",
                    likeCountUpdated, commentCountUpdated, commentLikeCountUpdated);
        }
    }
}
