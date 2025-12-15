package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.event.FeedDeletedEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 피드 삭제 관련 로직을 담당하는 Facade 서비스
 *
 * [리팩토링] 이벤트 기반 느슨한 결합 (Event-Driven Loose Coupling)
 * - 기존: NotificationService, CommentService, FeedLikeService 등 직접 의존 (7개 의존성)
 * - 변경: FeedRepository, CommentRepository만 의존 (2개 의존성)
 * - 관련 데이터 정리는 FeedDeletedEvent 리스너들이 각자 도메인을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedDeleteFacade {

    private final FeedRepository feedRepository;
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 피드 삭제 - Soft Delete 후 이벤트 발행
     * 관련 데이터 정리(댓글, 좋아요, 신고, 알림, 캐시, 포인트)는 이벤트 리스너에서 비동기 처리
     */
    @Transactional
    public void deleteFeed(User user, Long feedId) {
        Feed feed = feedRepository.findByIdForDelete(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        validateOwnership(feed, user);

        // 삭제 전에 관련 데이터 ID 및 정보 수집 (Soft Delete 후에는 조회 불가)
        List<Long> parentCommentIds = commentRepository.findParentCommentIdsByFeed(feed);
        List<Long> allCommentIds = commentRepository.findCommentIdsByFeed(feed);
        Long writerId = feed.getWriter().getId();
        long feedPoints = feed.getPoints();

        // Soft Delete
        feedRepository.delete(feed);

        // 트랜잭션 커밋 후 각 도메인별 리스너가 자신의 데이터 정리
        eventPublisher.publishEvent(FeedDeletedEvent.of(
                feedId,
                writerId,
                parentCommentIds,
                allCommentIds,
                feed.getCreatedAt(),
                feedPoints
        ));

        log.info("Feed soft deleted, event published: feedId={}, userId={}", feedId, user.getId());
    }

    private void validateOwnership(Feed feed, User user) {
        if (!Objects.equals(feed.getWriter().getId(), user.getId())) {
            throw new BusinessException(ErrorCode.FEED_NOT_OWNER);
        }
    }
}
