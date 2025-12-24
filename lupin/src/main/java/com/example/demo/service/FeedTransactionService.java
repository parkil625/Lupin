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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

        // 이미지 저장 (시간 정보 포함)
        addImages(savedFeed, startImageKey, endImageKey, otherImageKeys, 
                startTimeOpt.orElse(null), endTimeOpt.orElse(null));

        // 포인트 부여 [수정됨: addPoints -> earnPoints]
        if (workoutResult.score() > 0) {
            pointService.earnPoints(writer, workoutResult.score());
        }

        return savedFeed;
    }

    /**
     * 피드 수정 트랜잭션 로직
     */
    @Transactional
    public Feed updateFeed(User user, Long feedId, String content, String activity,
                           String startImageKey, String endImageKey, List<String> otherImageKeys,
                           Optional<LocalDateTime> startTimeOpt, Optional<LocalDateTime> endTimeOpt,
                           boolean imagesChanged) {

        Feed feed = feedRepository.findByIdWithWriter(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
        feed.validateOwner(user);

        // 1. URL 정규화 (변수 복구)
        String cleanStartKey = extractKey(startImageKey);
        String cleanEndKey = extractKey(endImageKey);

        // 2. 기존 이미지 시간 안전하게 찾기
        LocalDateTime existingStartTime = findCapturedAtSafe(feed, startImageKey);
        LocalDateTime existingEndTime = findCapturedAtSafe(feed, endImageKey);

        String oldActivity = feed.getActivity();
        long oldPoints = feed.getPoints();

        feed.update(content, activity);

        boolean activityChanged = !activity.equals(oldActivity);
        boolean shouldRecalculate = imagesChanged || activityChanged;

        LocalDateTime resolvedStartTime = null;
        LocalDateTime resolvedEndTime = null;

        if (shouldRecalculate) {
            if (imagesChanged) {
                // A. 이미지가 바뀌었으면 -> 프론트/S3에서 추출한 새 시간
                // 추출 실패 시, 요청된 키가 기존 이미지와 같다면 DB에 저장된 시간 사용
                resolvedStartTime = startTimeOpt.orElse(existingStartTime);
                resolvedEndTime = endTimeOpt.orElse(existingEndTime);
            } else {
                // B. 이미지는 그대로, 운동만 바뀐 경우 -> 기존 DB 시간
                resolvedStartTime = existingStartTime;
                resolvedEndTime = existingEndTime;
            }

            if (resolvedStartTime != null && resolvedEndTime != null) {
                WorkoutScoreService.WorkoutResult workoutResult =
                        workoutScoreService.validateAndCalculate(activity,
                                Optional.of(resolvedStartTime),
                                Optional.of(resolvedEndTime),
                                feed.getCreatedAt().toLocalDate());

                if (workoutResult.valid()) {
                    feed.updateScore((long) workoutResult.score(), workoutResult.calories());
                    pointService.adjustFeedPoints(user, oldPoints, workoutResult.score());
                } else {
                    // [수정] 0.0(double) -> 0(int) 타입 수정
                    feed.updateScore(0L, 0);
                    pointService.adjustFeedPoints(user, oldPoints, 0L);
                }
            } else {
                // [수정] 0.0(double) -> 0(int) 타입 수정
                feed.updateScore(0L, 0);
                pointService.adjustFeedPoints(user, oldPoints, 0L);
            }
        } else {
            // 변경사항 없음: 기존 시간 유지
            resolvedStartTime = existingStartTime;
            resolvedEndTime = existingEndTime;
        }

        feed.updateThumbnail(cleanStartKey);

        // 3. 이미지 정보 갱신 (이미지가 변경된 경우에만 수행)
        if (imagesChanged) {
            feed.updateThumbnail(cleanStartKey);
            feed.getImages().clear();
            addImages(feed, cleanStartKey, cleanEndKey, otherImageKeys, resolvedStartTime, resolvedEndTime);
        }
        // 이미지가 변경되지 않았다면 기존 DB에 있는 이미지와 시간을 그대로 유지합니다.
        
        feedRepository.flush();
        return feed;
    }

    // URL에서 순수 S3 Key만 추출하는 도우미 메서드
    private String extractKey(String urlOrKey) {
        if (urlOrKey == null) return null;
        if (urlOrKey.startsWith("http")) {
            // "com/" 뒷부분을 잘라내는 단순 파싱 예시 (실제 URL 구조에 맞춰 조정 필요)
            // 혹은 "/"로 split해서 뒤쪽 경로만 합치는 방식 등
            int index = urlOrKey.indexOf(".com/");
            if (index != -1) {
                return urlOrKey.substring(index + 5); // ".com/" 길이만큼 뒤로
            }
            // 만약 도메인이 다르다면 "amazonaws.com/" 등을 기준으로 자를 수도 있습니다.
            // 가장 확실한 건 DB에 저장되는 형태와 동일하게 만드는 것입니다.
        }
        return urlOrKey;
    }

    private void addImages(Feed feed, String startImageKey, String endImageKey, List<String> otherImageKeys,
                           LocalDateTime startTime, LocalDateTime endTime) {
        int sortOrder = 0;

        FeedImage startImg = FeedImage.builder()
                .feed(feed)
                .s3Key(startImageKey)
                .imgType(ImageType.START)
                .capturedAt(startTime)
                .sortOrder(sortOrder++)
                .build();
        feed.getImages().add(startImg);

        FeedImage endImg = FeedImage.builder()
                .feed(feed)
                .s3Key(endImageKey)
                .imgType(ImageType.END)
                .capturedAt(endTime)
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

    private LocalDateTime findCapturedAtSafe(Feed feed, String urlOrKey) {
        if (urlOrKey == null) return null;
        return feed.getImages().stream()
                .filter(img -> {
                    String dbKey = img.getS3Key();
                    // URL이나 Key가 서로 포함되어 있으면 같은 이미지로 판단
                    return urlOrKey.contains(dbKey) || dbKey.contains(urlOrKey);
                })
                .findFirst()
                .map(FeedImage::getCapturedAt)
                .orElse(null);
    }

}