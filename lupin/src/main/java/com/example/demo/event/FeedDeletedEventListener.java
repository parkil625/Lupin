package com.example.demo.event;

import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentReportRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedReportRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.service.LikeCountCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * 피드 삭제 이벤트 리스너
 *
 * [리팩토링] 이벤트 기반 느슨한 결합 (Event-Driven Loose Coupling)
 * - FeedDeleteFacade에서 직접 서비스를 호출하던 로직을 이벤트 리스너로 분리
 * - 각 도메인(댓글, 좋아요, 신고, 알림, 캐시)의 정리 작업을 담당
 * - REQUIRES_NEW 트랜잭션으로 피드 삭제와 독립적으로 실행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FeedDeletedEventListener {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentReportRepository commentReportRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedReportRepository feedReportRepository;
    private final NotificationRepository notificationRepository;
    private final LikeCountCacheService likeCountCacheService;

    /**
     * 피드 삭제 후 관련 데이터 정리
     * REQUIRES_NEW로 새 트랜잭션에서 실행하여 피드 삭제와 독립적으로 처리
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFeedDeletedEvent(FeedDeletedEvent event) {
        Long feedId = event.feedId();
        log.debug("Processing feed deletion cleanup: feedId={}", feedId);

        try {
            // 1. 알림 삭제 (피드 관련)
            cleanupNotifications(event);

            // 2. 댓글 관련 데이터 삭제
            cleanupComments(feedId);

            // 3. 피드 좋아요/신고 삭제
            cleanupFeedLikesAndReports(feedId);

            // 4. Redis 캐시 정리 (트랜잭션 외부 작업)
            cleanupCache(feedId);

            log.info("Feed deletion cleanup completed: feedId={}", feedId);

        } catch (Exception e) {
            log.error("Failed to cleanup after feed deletion: feedId={}", feedId, e);
            // 정리 실패는 치명적이지 않으므로 예외를 던지지 않음 (Eventual Consistency)
        }
    }

    private void cleanupNotifications(FeedDeletedEvent event) {
        Long feedId = event.feedId();
        String feedIdStr = String.valueOf(feedId);

        // FEED_LIKE, COMMENT 알림 삭제
        notificationRepository.deleteByRefIdAndType(feedIdStr, NotificationType.FEED_LIKE);
        notificationRepository.deleteByRefIdAndType(feedIdStr, NotificationType.COMMENT);

        // REPLY, COMMENT_LIKE 알림 삭제
        List<Long> parentCommentIds = event.parentCommentIds();
        List<Long> allCommentIds = event.allCommentIds();

        if (!parentCommentIds.isEmpty()) {
            List<String> parentIds = parentCommentIds.stream().map(String::valueOf).toList();
            notificationRepository.deleteByRefIdInAndType(parentIds, NotificationType.REPLY);
        }

        if (!allCommentIds.isEmpty()) {
            List<String> commentIds = allCommentIds.stream().map(String::valueOf).toList();
            notificationRepository.deleteByRefIdInAndType(commentIds, NotificationType.COMMENT_LIKE);
        }
    }

    private void cleanupComments(Long feedId) {
        // 댓글 좋아요 삭제
        commentLikeRepository.deleteByFeedId(feedId);
        // 댓글 신고 삭제
        commentReportRepository.deleteByFeedId(feedId);
        // 대댓글 삭제
        commentRepository.deleteRepliesByFeedId(feedId);
        // 부모 댓글 삭제
        commentRepository.deleteParentCommentsByFeedId(feedId);
    }

    private void cleanupFeedLikesAndReports(Long feedId) {
        feedLikeRepository.deleteByFeedId(feedId);
        feedReportRepository.deleteByFeedId(feedId);
    }

    private void cleanupCache(Long feedId) {
        try {
            likeCountCacheService.deleteLikeCount(feedId);
        } catch (Exception e) {
            log.warn("Failed to cleanup Redis cache: feedId={}", feedId, e);
        }
    }
}
