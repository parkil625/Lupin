package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ImageType;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 피드 트랜잭션 서비스
 * - FeedService에서 트랜잭션이 필요한 내부 로직을 분리
 * - Self-injection 패턴 대신 별도 서비스로 분리하여 AOP 프록시가 정상 동작하도록 함
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedTransactionService {

    private static final int MAX_WORKOUT_HOURS = 24;
    private static final int PHOTO_TIME_TOLERANCE_HOURS = 6;

    private final FeedRepository feedRepository;
    private final PointService pointService;
    private final WorkoutScoreService workoutScoreService;

    /**
     * 피드 생성 트랜잭션 로직
     */
    @Transactional
    public Feed createFeed(User writer, String activity, String content,
                           String startImageKey, String endImageKey, List<String> otherImageKeys,
                           Optional<LocalDateTime> startTimeOpt, Optional<LocalDateTime> endTimeOpt) {
        // 점수 계산
        int score = 0;
        int calories = 0;

        if (startTimeOpt.isPresent() && endTimeOpt.isPresent()) {
            LocalDateTime startTime = startTimeOpt.get();
            LocalDateTime endTime = endTimeOpt.get();

            if (isValidWorkoutTime(startTime, endTime, LocalDate.now())) {
                score = workoutScoreService.calculateScore(activity, startTime, endTime);
                calories = workoutScoreService.calculateCalories(activity, startTime, endTime);

                log.info("Workout verified: activity={}, duration={}min, score={}, calories={}",
                        activity, workoutScoreService.calculateDurationMinutes(startTime, endTime), score, calories);
            } else {
                log.warn("Workout time validation failed - score set to 0");
            }
        }

        // 피드 저장
        Feed feed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content(content)
                .points((long) score)
                .calories(calories)
                .build();

        Feed savedFeed = feedRepository.save(feed);
        savedFeed.setThumbnailUrl(startImageKey);

        // 이미지 저장
        addImages(savedFeed, startImageKey, endImageKey, otherImageKeys);

        // 포인트 부여
        if (score > 0) {
            pointService.addPoints(writer, score);
        }

        return savedFeed;
    }

    /**
     * 피드 수정 트랜잭션 로직
     */
    @Transactional
    public Feed updateFeed(User user, Long feedId, String content, String activity,
                           String startImageKey, String endImageKey, List<String> otherImageKeys,
                           Optional<LocalDateTime> startTimeOpt, Optional<LocalDateTime> endTimeOpt) {
        Feed feed = feedRepository.findByIdWithWriter(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        validateOwnership(feed, user);

        long oldPoints = feed.getPoints();
        feed.update(content, activity);

        // 점수 계산
        int score = 0;
        int calories = 0;

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

        feed.updateScore((long) score, calories);
        feed.setThumbnailUrl(startImageKey);

        // 기존 이미지 삭제 후 새 이미지 추가
        feed.getImages().clear();
        addImages(feed, startImageKey, endImageKey, otherImageKeys);

        // 포인트 조정
        if (oldPoints > 0) {
            pointService.deductPoints(user, oldPoints);
        }
        if (score > 0) {
            pointService.addPoints(user, score);
        }

        feedRepository.flush();
        feed.getImages().size();

        return feed;
    }

    private void addImages(Feed feed, String startImageKey, String endImageKey, List<String> otherImageKeys) {
        int sortOrder = 0;

        FeedImage startImg = FeedImage.builder()
                .feed(feed)
                .s3Key(startImageKey)
                .imgType(ImageType.START)
                .sortOrder(sortOrder++)
                .build();
        feed.getImages().add(startImg);

        FeedImage endImg = FeedImage.builder()
                .feed(feed)
                .s3Key(endImageKey)
                .imgType(ImageType.END)
                .sortOrder(sortOrder++)
                .build();
        feed.getImages().add(endImg);

        if (otherImageKeys != null) {
            for (String otherKey : otherImageKeys) {
                FeedImage otherImg = FeedImage.builder()
                        .feed(feed)
                        .s3Key(otherKey)
                        .imgType(ImageType.OTHER)
                        .sortOrder(sortOrder++)
                        .build();
                feed.getImages().add(otherImg);
            }
        }
    }

    private void validateOwnership(Feed feed, User user) {
        if (!Objects.equals(feed.getWriter().getId(), user.getId())) {
            throw new BusinessException(ErrorCode.FEED_NOT_OWNER);
        }
    }

    private boolean isValidWorkoutTime(LocalDateTime startTime, LocalDateTime endTime, LocalDate feedDate) {
        if (!startTime.isBefore(endTime)) {
            log.warn("Invalid workout time: start={} is not before end={}", startTime, endTime);
            return false;
        }

        if (Duration.between(startTime, endTime).toHours() > MAX_WORKOUT_HOURS) {
            log.warn("Workout too long: {} hours", Duration.between(startTime, endTime).toHours());
            return false;
        }

        LocalDateTime allowedStart = feedDate.atStartOfDay().minusHours(PHOTO_TIME_TOLERANCE_HOURS);
        LocalDateTime allowedEnd = feedDate.atTime(23, 59, 59).plusHours(PHOTO_TIME_TOLERANCE_HOURS);

        boolean startTimeValid = !startTime.isBefore(allowedStart) && !startTime.isAfter(allowedEnd);
        boolean endTimeValid = !endTime.isBefore(allowedStart) && !endTime.isAfter(allowedEnd);

        if (!startTimeValid || !endTimeValid) {
            log.warn("Photo time outside allowed range: start={}, end={}, allowed=[{} ~ {}]",
                    startTime, endTime, allowedStart, allowedEnd);
            return false;
        }

        return true;
    }
}
