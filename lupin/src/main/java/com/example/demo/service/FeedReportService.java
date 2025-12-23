package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedReport;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentLikeRepository;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedImageRepository;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedReportRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.domain.enums.NotificationType;
import com.example.demo.event.NotificationEvent;
import jakarta.persistence.EntityManager; // [추가]
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedReportService {

    private final FeedReportRepository feedReportRepository;
    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedImageRepository feedImageRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserPenaltyService userPenaltyService;
    private final EntityManager entityManager; // [추가]

    @Transactional
    public void toggleReport(User reporter, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (feedReportRepository.existsByReporterAndFeed(reporter, feed)) {
            feedReportRepository.deleteByReporterAndFeed(reporter, feed);
        } else {
            FeedReport feedReport = FeedReport.builder()
                    .reporter(reporter)
                    .feed(feed)
                    .build();
            feedReportRepository.save(feedReport);

            // [수정] 즉시 DB 반영 및 영속성 컨텍스트 분리 (삭제 시 충돌 방지)
            entityManager.flush();
            entityManager.detach(feedReport);

            checkAndApplyPenalty(feed);
        }
    }

    private void checkAndApplyPenalty(Feed feed) {
        long likeCount = feedLikeRepository.countByFeed(feed);
        long reportCount = feedReportRepository.countByFeed(feed);

        if (userPenaltyService.shouldApplyPenalty(likeCount, reportCount)) {
            User writer = feed.getWriter();
            if (!userPenaltyService.hasActivePenalty(writer, PenaltyType.FEED)) {
                userPenaltyService.addPenalty(writer, PenaltyType.FEED);
                deleteFeedByReport(feed);
                eventPublisher.publishEvent(NotificationEvent.feedDeleted(writer.getId()));
            }
        }
    }

    /**
     * 신고로 인한 피드 삭제 시 관련 알림 삭제
     * - FEED_LIKE: refId = feedId
     * - COMMENT: refId = feedId
     * - COMMENT_LIKE: refId = commentId
     * - REPLY: refId = parentCommentId (부모 댓글 ID)
     */
    private void deleteFeedByReport(Feed feed) {
        String feedIdStr = String.valueOf(feed.getId());

        // FEED_LIKE, COMMENT 알림 삭제 (refId = feedId)
        notificationRepository.deleteByRefIdAndType(feedIdStr, NotificationType.FEED_LIKE);
        notificationRepository.deleteByRefIdAndType(feedIdStr, NotificationType.COMMENT);

        // 댓글 ID 수집 (부모 댓글만)
        List<String> parentCommentIds = commentRepository.findByFeed(feed).stream()
                .filter(c -> c.getParent() == null)
                .map(c -> String.valueOf(c.getId()))
                .toList();

        // REPLY 알림 삭제 (refId = 부모 댓글 ID)
        if (!parentCommentIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(parentCommentIds, NotificationType.REPLY);
        }

        // 모든 댓글 ID 수집 (COMMENT_LIKE 삭제용)
        List<String> allCommentIds = commentRepository.findByFeed(feed).stream()
                .map(c -> String.valueOf(c.getId()))
                .toList();

        // COMMENT_LIKE 알림 삭제 (refId = commentId)
        if (!allCommentIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(allCommentIds, NotificationType.COMMENT_LIKE);
        }

        feedLikeRepository.deleteByFeed(feed);
        feedImageRepository.deleteByFeed(feed);
        feedReportRepository.deleteByFeed(feed);

        // [수정] 영속성 컨텍스트 초기화 (삭제 충돌 및 500 에러 방지)
        entityManager.flush();
        entityManager.clear();

        // 다시 조회해서 삭제 (안전하게 Soft Delete 수행)
        Feed targetFeed = feedRepository.findById(Long.parseLong(feedIdStr))
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        feedRepository.delete(targetFeed);
    }

    /**
     * 피드 삭제 시 신고 일괄 삭제
     */
    @Transactional
    public void deleteAllByFeed(Feed feed) {
        feedReportRepository.deleteByFeed(feed);
    }
}
