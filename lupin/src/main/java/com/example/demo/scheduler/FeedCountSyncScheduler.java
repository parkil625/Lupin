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

        if (likeCountUpdated > 0 || commentCountUpdated > 0) {
            log.info("피드 카운트 동기화 완료 - 좋아요: {}개, 댓글: {}개 업데이트",
                    likeCountUpdated, commentCountUpdated);
        }
    }
}
