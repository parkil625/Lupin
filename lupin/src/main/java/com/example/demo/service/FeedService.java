package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private static final int POINT_RECOVERY_DAYS = 7;

    private final FeedRepository feedRepository;
    private final PointService pointService;
    private final ImageMetadataService imageMetadataService;
    private final FeedTransactionService feedTransactionService;
    private final CommentService commentService;
    private final FeedLikeService feedLikeService;
    private final FeedReportService feedReportService;
    private final NotificationService notificationService;

    public Slice<Feed> getHomeFeeds(User user, int page, int size) {
        Slice<Feed> feeds = feedRepository.findByWriterNotOrderByIdDesc(user, PageRequest.of(page, size));
        feeds.getContent().forEach(feed -> feed.getImages().size());
        return feeds;
    }

    public Slice<Feed> getMyFeeds(User user, int page, int size) {
        Slice<Feed> feeds = feedRepository.findByWriterOrderByIdDesc(user, PageRequest.of(page, size));
        feeds.getContent().forEach(feed -> feed.getImages().size());
        return feeds;
    }

    @Transactional
    public Feed createFeed(User writer, String activity, String content) {
        return createFeed(writer, activity, content, List.of());
    }

    @Transactional
    public Feed createFeed(User writer, String activity, String content, List<String> s3Keys) {
        if (s3Keys.size() < 2) {
            throw new BusinessException(ErrorCode.FEED_IMAGES_REQUIRED);
        }
        String startImageKey = s3Keys.get(0);
        String endImageKey = s3Keys.get(1);
        List<String> otherImageKeys = s3Keys.size() > 2 ? s3Keys.subList(2, s3Keys.size()) : List.of();
        return createFeed(writer, activity, content, startImageKey, endImageKey, otherImageKeys);
    }

    /**
     * 피드 생성 (S3 I/O를 트랜잭션 외부에서 수행)
     */
    public Feed createFeed(User writer, String activity, String content, String startImageKey, String endImageKey, List<String> otherImageKeys) {
        if (startImageKey == null || endImageKey == null) {
            throw new BusinessException(ErrorCode.FEED_IMAGES_REQUIRED);
        }

        // [트랜잭션 외부] S3에서 EXIF 시간 추출 (네트워크 I/O)
        Optional<LocalDateTime> startTimeOpt = imageMetadataService.extractPhotoDateTime(startImageKey);
        Optional<LocalDateTime> endTimeOpt = imageMetadataService.extractPhotoDateTime(endImageKey);

        log.info("EXIF extraction completed: startImage={}, endImage={}",
                startTimeOpt.isPresent() ? "OK" : "NOT_FOUND",
                endTimeOpt.isPresent() ? "OK" : "NOT_FOUND");

        // [트랜잭션 내부] 별도 서비스로 분리하여 트랜잭션 적용
        return feedTransactionService.createFeed(writer, activity, content, startImageKey, endImageKey, otherImageKeys, startTimeOpt, endTimeOpt);
    }

    @Transactional
    public Feed updateFeed(User user, Long feedId, String content, String activity) {
        Feed feed = feedRepository.findByIdWithWriter(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        validateOwnership(feed, user);
        feed.update(content, activity);
        feed.getImages().size();
        return feed;
    }

    @Transactional
    public Feed updateFeed(User user, Long feedId, String content, String activity, List<String> s3Keys) {
        String startImageKey = s3Keys.get(0);
        String endImageKey = s3Keys.get(1);
        List<String> otherImageKeys = s3Keys.size() > 2 ? s3Keys.subList(2, s3Keys.size()) : List.of();
        return updateFeed(user, feedId, content, activity, startImageKey, endImageKey, otherImageKeys);
    }

    /**
     * 피드 수정 (S3 I/O를 트랜잭션 외부에서 수행)
     */
    public Feed updateFeed(User user, Long feedId, String content, String activity, String startImageKey, String endImageKey, List<String> otherImageKeys) {
        // [트랜잭션 외부] S3에서 EXIF 시간 추출 (네트워크 I/O)
        Optional<LocalDateTime> startTimeOpt = imageMetadataService.extractPhotoDateTime(startImageKey);
        Optional<LocalDateTime> endTimeOpt = imageMetadataService.extractPhotoDateTime(endImageKey);

        log.info("Feed update - EXIF extraction completed: startImage={}, endImage={}",
                startTimeOpt.isPresent() ? "OK" : "NOT_FOUND",
                endTimeOpt.isPresent() ? "OK" : "NOT_FOUND");

        // [트랜잭션 내부] 별도 서비스로 분리하여 트랜잭션 적용
        return feedTransactionService.updateFeed(user, feedId, content, activity, startImageKey, endImageKey, otherImageKeys, startTimeOpt, endTimeOpt);
    }

    @Transactional
    public void deleteFeed(User user, Long feedId) {
        Feed feed = feedRepository.findByIdForDelete(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        validateOwnership(feed, user);
        recoverPointsIfWithinPeriod(feed);

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

        feedRepository.delete(feed);
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

        LocalDateTime recoveryDeadline = LocalDateTime.now().minusDays(POINT_RECOVERY_DAYS);
        if (feed.getCreatedAt().isAfter(recoveryDeadline)) {
            pointService.deductPoints(feed.getWriter(), feed.getPoints());
        }
    }

    public Feed getFeedDetail(Long feedId) {
        Feed feed = feedRepository.findByIdWithWriter(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        feed.getImages().size();
        return feed;
    }

    public boolean canPostToday(User user) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        return !feedRepository.existsByWriterAndCreatedAtBetween(user, startOfDay, endOfDay);
    }

    /**
     * 피드 목록에서 작성자별 이번 달 활동일수를 배치로 조회
     */
    public Map<Long, Integer> getActiveDaysMap(List<Feed> feeds) {
        if (feeds.isEmpty()) {
            return Map.of();
        }

        List<Long> writerIds = feeds.stream()
                .map(feed -> feed.getWriter().getId())
                .distinct()
                .toList();

        YearMonth currentMonth = YearMonth.now();
        LocalDateTime startOfMonth = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59);

        List<Object[]> results = feedRepository.countActiveDaysByWriterIds(writerIds, startOfMonth, endOfMonth);

        Map<Long, Integer> activeDaysMap = new HashMap<>();
        for (Object[] row : results) {
            Long writerId = (Long) row[0];
            Long activeDays = (Long) row[1];
            activeDaysMap.put(writerId, activeDays.intValue());
        }
        return activeDaysMap;
    }
}
