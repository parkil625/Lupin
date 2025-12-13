package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import com.example.demo.event.NotificationEvent;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.domain.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedLikeService {

    private final FeedLikeRepository feedLikeRepository;
    private final FeedRepository feedRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final LikeCountCacheService likeCountCacheService;

    @Transactional
    public FeedLike likeFeed(User user, Long feedId) {
        // [최적화] ID 기반 존재 확인 - 객체 로딩 없이 바로 체크
        if (feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feedId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        FeedLike feedLike = FeedLike.builder()
                .user(user)
                .feed(feed)
                .build();

        FeedLike savedFeedLike = feedLikeRepository.save(feedLike);

        // [Hot Write 최적화] Redis 카운트 증가 → 스케줄러가 DB 동기화
        likeCountCacheService.incrementLikeCount(feedId);

        // [최적화] 이벤트 발행 - 트랜잭션 커밋 후 비동기 알림 처리
        eventPublisher.publishEvent(NotificationEvent.feedLike(
                feed.getWriter().getId(),
                user.getId(),
                user.getName(),
                user.getAvatar(),
                feedId,
                feed.getContent()
        ));

        return savedFeedLike;
    }

    @Transactional
    public void unlikeFeed(User user, Long feedId) {
        if (!feedRepository.existsById(feedId)) {
            throw new BusinessException(ErrorCode.FEED_NOT_FOUND);
        }

        // user.getId()만 사용하여 detached entity (@Version null) 문제 방지
        FeedLike feedLike = feedLikeRepository.findByUserIdAndFeedId(user.getId(), feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND));

        // [Hot Write 최적화] Redis 카운트 감소 → 스케줄러가 DB 동기화
        likeCountCacheService.decrementLikeCount(feedId);

        // refId = feedId (피드 참조)
        notificationRepository.deleteByRefIdAndType(String.valueOf(feedId), NotificationType.FEED_LIKE);
        feedLikeRepository.delete(feedLike);
    }

    /**
     * 피드 삭제 시 좋아요 일괄 삭제
     */
    @Transactional
    public void deleteAllByFeed(Feed feed) {
        feedLikeRepository.deleteByFeed(feed);
    }

    /**
     * 사용자의 피드 좋아요 여부 확인
     */
    public boolean isLiked(Long userId, Long feedId) {
        return feedLikeRepository.existsByUserIdAndFeedId(userId, feedId);
    }

    /**
     * 피드 좋아요 ID로 피드 ID 조회
     */
    public Long getFeedIdByFeedLikeId(Long feedLikeId) {
        return feedLikeRepository.findById(feedLikeId)
                .map(feedLike -> feedLike.getFeed().getId())
                .orElse(null);
    }
}
