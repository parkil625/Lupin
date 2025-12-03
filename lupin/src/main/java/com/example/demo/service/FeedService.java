package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ImageType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.CommentRepository;
import com.example.demo.repository.FeedImageRepository;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedReportRepository;
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
import java.util.Objects;

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
    private final FeedLikeRepository feedLikeRepository;
    private final FeedReportRepository feedReportRepository;
    private final CommentRepository commentRepository;
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

        // EXIF 시간 추출 시도 및 점수 계산
        int score = 0;
        int calories = 0;

        var startTimeOpt = imageMetadataService.extractPhotoDateTime(startImageKey);
        var endTimeOpt = imageMetadataService.extractPhotoDateTime(endImageKey);

        if (startTimeOpt.isPresent() && endTimeOpt.isPresent()) {
            LocalDateTime startTime = startTimeOpt.get();
            LocalDateTime endTime = endTimeOpt.get();

            // 시간 유효성 검증 (실패 시 점수=0)
            if (isValidWorkoutTime(startTime, endTime, LocalDate.now())) {
                // 점수 계산
                score = workoutScoreService.calculateScore(activity, startTime, endTime);
                calories = workoutScoreService.calculateCalories(activity, startTime, endTime);

                log.info("Workout verified: activity={}, duration={}min, score={}, calories={}",
                        activity, workoutScoreService.calculateDurationMinutes(startTime, endTime), score, calories);
            } else {
                log.warn("Workout time validation failed - score set to 0");
            }
        } else {
            log.warn("EXIF time not found: startImage={}, endImage={}",
                    startTimeOpt.isPresent() ? "OK" : "NOT_FOUND",
                    endTimeOpt.isPresent() ? "OK" : "NOT_FOUND");
            // EXIF 없으면 점수=0으로 피드 생성
        }

        // 피드 저장 (EXIF 없어도 점수=0으로 저장)
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
    public Feed updateFeed(User user, Long feedId, String content, String activity, List<String> s3Keys) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        validateOwnership(feed, user);

        // 기존 포인트 저장 (포인트 차액 계산용)
        long oldPoints = feed.getPoints() != null ? feed.getPoints() : 0L;

        // 내용/운동종류 업데이트
        feed.update(content, activity);

        // EXIF 시간 추출 및 점수 계산
        int score = 0;
        int calories = 0;

        var startTimeOpt = imageMetadataService.extractPhotoDateTime(s3Keys.get(0));
        var endTimeOpt = imageMetadataService.extractPhotoDateTime(s3Keys.get(1));

        if (startTimeOpt.isPresent() && endTimeOpt.isPresent()) {
            LocalDateTime startTime = startTimeOpt.get();
            LocalDateTime endTime = endTimeOpt.get();

            if (isValidWorkoutTime(startTime, endTime, LocalDate.now())) {
                score = workoutScoreService.calculateScore(activity, startTime, endTime);
                calories = workoutScoreService.calculateCalories(activity, startTime, endTime);

                log.info("Feed update - Workout verified: activity={}, duration={}min, score={}, calories={}",
                        activity, workoutScoreService.calculateDurationMinutes(startTime, endTime), score, calories);
            } else {
                log.warn("Feed update - Workout time validation failed - score set to 0");
            }
        }

        // 피드 점수/칼로리 업데이트
        feed.updateScore((long) score, calories);

        // 기존 이미지 삭제
        feedImageRepository.deleteByFeed(feed);

        // 새 이미지 저장
        for (int i = 0; i < s3Keys.size(); i++) {
            ImageType imgType = (i == 0) ? ImageType.START : (i == 1) ? ImageType.END : ImageType.OTHER;
            FeedImage feedImage = FeedImage.builder()
                    .feed(feed)
                    .s3Key(s3Keys.get(i))
                    .imgType(imgType)
                    .sortOrder(i)
                    .build();
            feedImageRepository.save(feedImage);
        }

        // 포인트 조정 (기존 차감 후 새로 적립)
        if (oldPoints > 0) {
            pointService.deductPoints(user, oldPoints);
        }
        if (score > 0) {
            pointService.addPoints(user, score);
        }

        return feed;
    }

    @Transactional
    public void deleteFeed(User user, Long feedId) {
        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        validateOwnership(feed, user);
        recoverPointsIfWithinPeriod(feed);

        // 관련 데이터 삭제 (외래 키 제약 조건 순서대로)
        commentRepository.deleteByFeed(feed);
        feedLikeRepository.deleteByFeed(feed);
        feedReportRepository.deleteByFeed(feed);
        notificationRepository.deleteByRefIdAndTypeIn(String.valueOf(feedId), FEED_NOTIFICATION_TYPES);
        feedImageRepository.deleteByFeed(feed);
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
     * 운동 시간 유효성 검증 (유효하면 true, 아니면 false)
     * - 시작 시간이 끝 시간보다 먼저여야 함
     * - 운동 시간이 24시간을 초과하면 안 됨
     * - 사진 시간이 당일(±오차범위) 내여야 함
     */
    private boolean isValidWorkoutTime(LocalDateTime startTime, LocalDateTime endTime, LocalDate feedDate) {
        // 시작 시간이 끝 시간보다 같거나 늦으면 무효
        if (!startTime.isBefore(endTime)) {
            log.warn("Invalid workout time: start={} is not before end={}", startTime, endTime);
            return false;
        }

        // 운동 시간이 24시간 초과하면 무효
        if (Duration.between(startTime, endTime).toHours() > MAX_WORKOUT_HOURS) {
            log.warn("Workout too long: {} hours", Duration.between(startTime, endTime).toHours());
            return false;
        }

        // 사진 시간이 당일(±오차범위) 내인지 검증
        LocalDateTime allowedStart = feedDate.atStartOfDay().minusHours(PHOTO_TIME_TOLERANCE_HOURS);
        LocalDateTime allowedEnd = feedDate.atTime(23, 59, 59).plusHours(PHOTO_TIME_TOLERANCE_HOURS);

        boolean startTimeValid = !startTime.isBefore(allowedStart) && !startTime.isAfter(allowedEnd);
        boolean endTimeValid = !endTime.isBefore(allowedStart) && !endTime.isAfter(allowedEnd);

        log.info("Workout time validation: feedDate={}, allowedRange=[{} ~ {}], startTime={} (valid={}), endTime={} (valid={})",
                feedDate, allowedStart, allowedEnd, startTime, startTimeValid, endTime, endTimeValid);

        if (!startTimeValid || !endTimeValid) {
            log.warn("Photo time outside allowed range: start={}, end={}, allowed=[{} ~ {}]",
                    startTime, endTime, allowedStart, allowedEnd);
            return false;
        }

        return true;
    }
}
