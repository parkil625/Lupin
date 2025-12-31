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
    private final NotificationSseService notificationSseService; // [추가] 주입

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

        // 1. 좋아요 삭제 진행
        feedLikeRepository.delete(feedLike);
        // [디버깅] 좋아요 삭제 완료
        // log.debug("Deleted FeedLike for userId: {}, feedId: {}", user.getId(), feedId);

        // 2. 남은 좋아요 개수 확인 (스마트한 알림 처리)
        long remainingLikes = feedLikeRepository.countByFeedId(feedId);

        if (remainingLikes == 0) {
            // [수정] 남은 좋아요가 없으면 알림 조회 -> SSE 전송 -> 삭제
            String refId = String.valueOf(feedId);

            // 1. 삭제할 알림 조회 (피드 주인에게 간 알림)
            java.util.List<com.example.demo.domain.entity.Notification> targets = notificationRepository.findByRefIdAndType(refId, NotificationType.FEED_LIKE);

            if (!targets.isEmpty()) {
                // 2. 알림 수신자(피드 주인)에게 삭제 이벤트 전송
                Long receiverId = targets.get(0).getUser().getId();
                java.util.List<Long> ids = targets.stream().map(com.example.demo.domain.entity.Notification::getId).toList();

                // SSE로 "이 알림 지워라" 명령 전송
                notificationSseService.sendNotificationDelete(receiverId, ids);
                // [디버깅]
                // log.debug("Sending delete event for feed like notifications: userId={}, ids={}", receiverId, ids);

                // 3. DB 삭제
                notificationRepository.deleteAll(targets);
            }
        } else {
            // 남은 좋아요가 있다면, 가장 최근에 누른 사람을 찾아 알림 갱신 이벤트 발행
            feedLikeRepository.findTopByFeedIdOrderByCreatedAtDesc(feedId)
                    .ifPresent(latestLike -> {
                        Feed feed = latestLike.getFeed();
                        User latestLiker = latestLike.getUser();

                        // 알림 갱신을 위해 이벤트 재발행 (Listener가 "뭉치기" 로직 수행)
                        eventPublisher.publishEvent(NotificationEvent.feedLike(
                                feed.getWriter().getId(),
                                latestLiker.getId(), // 이제 이 사람이 주인공
                                latestLiker.getName(),
                                latestLiker.getAvatar(),
                                feedId,
                                feed.getContent()
                        ));
                    });
        }
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
