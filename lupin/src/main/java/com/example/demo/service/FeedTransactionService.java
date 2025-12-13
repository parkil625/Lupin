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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
        // 점수 계산 (WorkoutScoreService에 위임)
        WorkoutScoreService.WorkoutResult workoutResult =
                workoutScoreService.validateAndCalculate(activity, startTimeOpt, endTimeOpt, LocalDate.now());

        if (!workoutResult.valid() && (startTimeOpt.isPresent() || endTimeOpt.isPresent())) {
            log.warn("Workout time validation failed - score set to 0");
        }

        // 피드 저장
        Feed feed = Feed.builder()
                .writer(writer)
                .activity(activity)
                .content(content)
                .points((long) workoutResult.score())
                .calories(workoutResult.calories())
                .build();

        Feed savedFeed = feedRepository.save(feed);
        savedFeed.updateThumbnail(startImageKey);

        // 이미지 저장
        addImages(savedFeed, startImageKey, endImageKey, otherImageKeys);

        // 포인트 부여
        if (workoutResult.score() > 0) {
            pointService.addPoints(writer, workoutResult.score());
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

        feed.validateOwner(user);

        long oldPoints = feed.getPoints();
        feed.update(content, activity);

        // 점수 계산 (WorkoutScoreService에 위임)
        WorkoutScoreService.WorkoutResult workoutResult =
                workoutScoreService.validateAndCalculate(activity, startTimeOpt, endTimeOpt, LocalDate.now());

        if (!workoutResult.valid() && (startTimeOpt.isPresent() || endTimeOpt.isPresent())) {
            log.warn("Feed update - Workout time validation failed - score set to 0");
        }

        feed.updateScore((long) workoutResult.score(), workoutResult.calories());
        feed.updateThumbnail(startImageKey);

        // 기존 이미지 삭제 후 새 이미지 추가
        feed.getImages().clear();
        addImages(feed, startImageKey, endImageKey, otherImageKeys);

        // 포인트 조정 (PointService에 위임)
        pointService.adjustFeedPoints(user, oldPoints, workoutResult.score());

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
}
