package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedReport;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedImageRepository;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedReportRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
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
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final UserPenaltyService userPenaltyService;

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
                notificationService.createFeedDeletedByReportNotification(writer);
            }
        }
    }

    private void deleteFeedByReport(Feed feed) {
        // FEED_LIKE 알림 삭제 (refId = FeedLike ID)
        List<String> feedLikeIds = feedLikeRepository.findByFeed(feed).stream()
                .map(fl -> String.valueOf(fl.getId()))
                .toList();
        if (!feedLikeIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(feedLikeIds, "FEED_LIKE");
        }

        // COMMENT 알림 삭제 (refId = Comment ID)
        List<String> commentIds = commentRepository.findByFeed(feed).stream()
                .map(c -> String.valueOf(c.getId()))
                .toList();
        if (!commentIds.isEmpty()) {
            notificationRepository.deleteByRefIdInAndType(commentIds, "COMMENT");
        }

        feedLikeRepository.deleteByFeed(feed);
        feedImageRepository.deleteByFeed(feed);
        feedReportRepository.deleteByFeed(feed);
        feedRepository.delete(feed);
    }
}
