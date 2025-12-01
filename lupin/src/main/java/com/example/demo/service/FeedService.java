package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ImageType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedImageRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private static final int POINT_RECOVERY_DAYS = 7;
    private static final List<String> FEED_NOTIFICATION_TYPES = List.of("FEED_LIKE", "COMMENT");

    private final FeedRepository feedRepository;
    private final FeedImageRepository feedImageRepository;
    private final PointService pointService;
    private final NotificationRepository notificationRepository;

    public Slice<Feed> getHomeFeeds(User user, int page, int size) {
        return feedRepository.findByWriterNotOrderByIdDesc(user, PageRequest.of(page, size));
    }

    public Slice<Feed> getMyFeeds(User user, int page, int size) {
        return feedRepository.findByWriterOrderByIdDesc(user, PageRequest.of(page, size));
    }

    @Transactional
    public Feed createFeed(User writer, String activity, String content) {
        return createFeed(writer, activity, content, List.of());
    }

    @Transactional
    public Feed createFeed(User writer, String activity, String content, List<String> s3Keys) {
        Feed feed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content(content)
                .build();

        Feed savedFeed = feedRepository.save(feed);

        for (int i = 0; i < s3Keys.size(); i++) {
            FeedImage feedImage = FeedImage.builder()
                    .feed(savedFeed)
                    .s3Key(s3Keys.get(i))
                    .imgType(ImageType.OTHER)
                    .sortOrder(i)
                    .build();
            feedImageRepository.save(feedImage);
        }

        return savedFeed;
    }

    @Transactional
    public Feed updateFeed(User user, Long feedId, String content, String activity) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        validateOwnership(feed, user);
        feed.update(content, activity);
        return feed;
    }

    @Transactional
    public void deleteFeed(User user, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        validateOwnership(feed, user);
        recoverPointsIfWithinPeriod(feed);
        notificationRepository.deleteByRefIdAndTypeIn(String.valueOf(feedId), FEED_NOTIFICATION_TYPES);
        feedImageRepository.deleteByFeed(feed);
        feedRepository.delete(feed);
    }

    private void validateOwnership(Feed feed, User user) {
        if (!feed.getWriter().equals(user)) {
            throw new BusinessException(ErrorCode.FEED_NOT_OWNER);
        }
    }

    private void recoverPointsIfWithinPeriod(Feed feed) {
        if (feed.getPoints() <= 0) {
            return;
        }

        LocalDateTime recoveryDeadline = LocalDateTime.now().minusDays(POINT_RECOVERY_DAYS);
        if (feed.getCreatedAt().isAfter(recoveryDeadline)) {
            pointService.deductPoints(feed.getWriter(), feed.getPoints());
        }
    }

    public Feed getFeedDetail(Long feedId) {
        return feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
    }

    public boolean canPostToday(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        return !feedRepository.existsByWriterAndCreatedAtBetween(user, startOfDay, endOfDay);
    }
}
