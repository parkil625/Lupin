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

        // 좋아요 카운트 증가 (반정규화)
        feed.incrementLikeCount();

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
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        FeedLike feedLike = feedLikeRepository.findByUserAndFeed(user, feed)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND));

        // 좋아요 카운트 감소 (반정규화)
        feed.decrementLikeCount();

        // refId = feedId (피드 참조)
        notificationRepository.deleteByRefIdAndType(String.valueOf(feedId), "FEED_LIKE");
        feedLikeRepository.delete(feedLike);
    }
}
