package com.example.demo.service;

import com.example.demo.config.properties.FeedProperties;
import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.event.FeedDeletedEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 피드 삭제 관련 로직을 담당하는 Facade 서비스
 * FeedService의 책임 분리 및 관심사 분리를 위해 별도 서비스로 추출
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedDeleteFacade {

    private final FeedProperties feedProperties;
    private final FeedRepository feedRepository;
    private final PointService pointService;
    private final CommentService commentService;
    private final FeedLikeService feedLikeService;
    private final FeedReportService feedReportService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 피드 삭제 - 관련된 모든 데이터(댓글, 좋아요, 신고, 알림)를 함께 삭제
     * 트랜잭션 커밋 후 FeedDeletedEvent 발행하여 캐시 정리 등 비동기 처리
     */
    @Transactional
    public void deleteFeed(User user, Long feedId) {
        Feed feed = feedRepository.findByIdForDelete(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        Long writerId = feed.getWriter().getId();
        validateOwnership(feed, user);
        recoverPointsIfWithinPeriod(feed);
        deleteRelatedData(feed, feedId);
        feedRepository.delete(feed);

        // 트랜잭션 커밋 후 비동기 정리 작업을 위한 이벤트 발행
        eventPublisher.publishEvent(FeedDeletedEvent.of(feedId, writerId));

        log.info("Feed deleted: feedId={}, userId={}", feedId, user.getId());
    }

    private void validateOwnership(Feed feed, User user) {
        if (!Objects.equals(feed.getWriter().getId(), user.getId())) {
            throw new BusinessException(ErrorCode.FEED_NOT_OWNER);
        }
    }

    private void recoverPointsIfWithinPeriod(Feed feed) {
        if (feed.getPoints() <= 0) {
            return;
        }

        LocalDateTime recoveryDeadline = LocalDateTime.now().minusDays(feedProperties.getPointRecoveryDays());
        if (feed.getCreatedAt().isAfter(recoveryDeadline)) {
            pointService.deductPoints(feed.getWriter(), feed.getPoints());
            log.debug("Points recovered for deleted feed: feedId={}, points={}", feed.getId(), feed.getPoints());
        }
    }

    private void deleteRelatedData(Feed feed, Long feedId) {
        // 알림 삭제 (피드 관련)
        notificationService.deleteFeedRelatedNotifications(feedId);

        // 댓글 삭제 및 관련 알림 삭제
        CommentService.CommentDeleteResult commentResult = commentService.deleteAllByFeed(feed);
        notificationService.deleteCommentRelatedNotifications(
                commentResult.parentCommentIds(),
                commentResult.allCommentIds()
        );

        // 좋아요, 신고 삭제
        feedLikeService.deleteAllByFeed(feed);
        feedReportService.deleteAllByFeed(feed);

        // Redis 캐시 삭제는 FeedDeletedEvent 리스너에서 트랜잭션 커밋 후 처리
    }
}
