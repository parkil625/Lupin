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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private static final int POINT_RECOVERY_DAYS = 7;
    private static final int MAX_WORKOUT_HOURS = 24;
    private static final int PHOTO_TIME_TOLERANCE_HOURS = 6; // 자정 넘어 운동 허용 (±6시간)
    private static final List<String> FEED_NOTIFICATION_TYPES = List.of("FEED_LIKE", "COMMENT");

    private final FeedRepository feedRepository;
    private final FeedImageRepository feedImageRepository;
    private final PointService pointService;
    private final NotificationRepository notificationRepository;
    private final ImageMetadataService imageMetadataService;
    private final WorkoutScoreService workoutScoreService;

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
        // 시작/끝 사진 필수 검증
        if (s3Keys.size() < 2) {
            throw new BusinessException(ErrorCode.FEED_IMAGES_REQUIRED);
        }

        String startImageKey = s3Keys.get(0);
        String endImageKey = s3Keys.get(1);

        // EXIF 시간 추출
        LocalDateTime startTime = imageMetadataService.extractPhotoDateTime(startImageKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_PHOTO_TIME_NOT_FOUND));
        LocalDateTime endTime = imageMetadataService.extractPhotoDateTime(endImageKey)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_PHOTO_TIME_NOT_FOUND));

        // 시작 시간이 끝 시간보다 같거나 늦으면 예외
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.FEED_INVALID_PHOTO_TIME);
        }

        // 운동 시간이 24시간 초과하면 예외
        if (Duration.between(startTime, endTime).toHours() > MAX_WORKOUT_HOURS) {
            throw new BusinessException(ErrorCode.FEED_WORKOUT_TOO_LONG);
        }

        // 사진 시간이 당일(±오차범위) 내인지 검증
        validatePhotoTimeIsToday(startTime, endTime, LocalDate.now());

        // 점수 및 칼로리 계산
        int score = workoutScoreService.calculateScore(activity, startTime, endTime);
        int calories = workoutScoreService.calculateCalories(activity, startTime, endTime);

        log.info("Workout verified: activity={}, duration={}min, score={}, calories={}",
                activity, workoutScoreService.calculateDurationMinutes(startTime, endTime), score, calories);

        // 피드 저장
        Feed feed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content(content)
                .points((long) score)
                .calories(calories)
                .build();

        Feed savedFeed = feedRepository.save(feed);

        // 이미지 저장 (시작=START, 끝=END, 나머지=OTHER)
        for (int i = 0; i < s3Keys.size(); i++) {
            ImageType imgType = (i == 0) ? ImageType.START : (i == 1) ? ImageType.END : ImageType.OTHER;
            FeedImage feedImage = FeedImage.builder()
                    .feed(savedFeed)
                    .s3Key(s3Keys.get(i))
                    .imgType(imgType)
                    .sortOrder(i)
                    .build();
            feedImageRepository.save(feedImage);
        }

        // 포인트 부여
        if (score > 0) {
            pointService.addPoints(writer, score);
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

    /**
     * 사진 시간이 피드 작성일 기준 당일(±오차범위) 내인지 검증
     * 예: 12월 1일 피드 → 11/30 18:00 ~ 12/2 05:59 사이 사진만 허용 (±6시간)
     */
    private void validatePhotoTimeIsToday(LocalDateTime startTime, LocalDateTime endTime, LocalDate feedDate) {
        LocalDateTime allowedStart = feedDate.atStartOfDay().minusHours(PHOTO_TIME_TOLERANCE_HOURS);
        LocalDateTime allowedEnd = feedDate.atTime(23, 59, 59).plusHours(PHOTO_TIME_TOLERANCE_HOURS);

        boolean startTimeValid = !startTime.isBefore(allowedStart) && !startTime.isAfter(allowedEnd);
        boolean endTimeValid = !endTime.isBefore(allowedStart) && !endTime.isAfter(allowedEnd);

        if (!startTimeValid || !endTimeValid) {
            throw new BusinessException(ErrorCode.FEED_PHOTO_NOT_TODAY);
        }
    }
}
