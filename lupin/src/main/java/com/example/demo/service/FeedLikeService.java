package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedLikeService {

    private final FeedLikeRepository feedLikeRepository;
    private final FeedRepository feedRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @Transactional
    public FeedLike likeFeed(User user, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        if (feedLikeRepository.existsByUserAndFeed(user, feed)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        FeedLike feedLike = FeedLike.builder()
                .user(user)
                .feed(feed)
                .build();

        FeedLike savedFeedLike = feedLikeRepository.save(feedLike);
        notificationService.createFeedLikeNotification(feed.getWriter(), user, savedFeedLike.getId());

        return savedFeedLike;
    }

    @Transactional
    public void unlikeFeed(User user, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        FeedLike feedLike = feedLikeRepository.findByUserAndFeed(user, feed)
                .orElseThrow(() -> new BusinessException(ErrorCode.LIKE_NOT_FOUND));

        notificationRepository.deleteByRefIdAndType(String.valueOf(feedLike.getId()), "FEED_LIKE");
        feedLikeRepository.delete(feedLike);
    }
}
