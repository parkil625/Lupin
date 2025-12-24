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
                        boolean imagesChanged) { // [추가] 파라미터
        Feed feed = feedRepository.findByIdWithWriter(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        feed.validateOwner(user);

        // URL 정규화 (프론트에서 풀 경로가 와도 처리 가능하도록)
        String cleanStartKey = extractKey(startImageKey);
        String cleanEndKey = extractKey(endImageKey);

        String oldActivity = feed.getActivity();
        long oldPoints = feed.getPoints();

        // 1. 기존 이미지 시간 정보 백업
        Map<String, LocalDateTime> oldTimeMap = feed.getImages().stream()
                .filter(img -> img.getCapturedAt() != null)
                .collect(Collectors.toMap(FeedImage::getS3Key, FeedImage::getCapturedAt, (a, b) -> a));

        feed.update(content, activity);

        // 2. 점수 재계산 여부 판단 (이미지가 바뀌었거나, 운동 종류가 바뀌었을 때)
        boolean activityChanged = !activity.equals(oldActivity);
        boolean shouldRecalculate = imagesChanged || activityChanged;

        LocalDateTime resolvedStartTime = null;
        LocalDateTime resolvedEndTime = null;

        if (shouldRecalculate) {
            // 시간 결정: 이미지가 바뀌었으면 새 추출값, 아니면(운동만 변경) 기존 값 사용
            if (imagesChanged) {
                resolvedStartTime = startTimeOpt.orElse(null);
                resolvedEndTime = endTimeOpt.orElse(null);
            } else {
                // 이미지는 그대로인데 운동만 바뀐 경우 -> 기존 DB 시간 사용
                resolvedStartTime = oldTimeMap.get(cleanStartKey);
                resolvedEndTime = oldTimeMap.get(cleanEndKey);
            }

            // 시간 정보가 없으면 계산 불가 (기존 유지 or 에러)
            // 여기서는 운동 종류를 바꿨는데 시간이 없으면 0점이 될 수 있으므로 방어 로직 필요
            if (resolvedStartTime != null && resolvedEndTime != null) {
                WorkoutScoreService.WorkoutResult workoutResult =
                        workoutScoreService.validateAndCalculate(activity, 
                                Optional.of(resolvedStartTime), 
                                Optional.of(resolvedEndTime), 
                                feed.getCreatedAt().toLocalDate());

                if (workoutResult.valid()) {
                    feed.updateScore((long) workoutResult.score(), workoutResult.calories());
                    pointService.adjustFeedPoints(user, oldPoints, workoutResult.score());
                }
            } else {
                log.warn("Feed update score skipped: Time metadata missing. imagesChanged={}, activityChanged={}", imagesChanged, activityChanged);
                // 이미지가 변경되었는데 시간이 없으면 에러 처리가 나을 수 있음
                if (imagesChanged) {
                    throw new BusinessException(ErrorCode.FEED_IMAGES_REQUIRED);
                }
                // 운동만 바꿨는데 시간이 없으면? 기존 점수 유지할지 0점 만들지 정책 결정 필요.
                // 현재는 기존 점수 유지 (updateScore 호출 안함)
            }
        } else {
            // 변경사항 없음 -> 시간 정보 복구 (DB 유지를 위해)
            resolvedStartTime = oldTimeMap.get(cleanStartKey);
            resolvedEndTime = oldTimeMap.get(cleanEndKey);
        }

        feed.updateThumbnail(cleanStartKey);

        // 3. 이미지 정보 갱신
        feed.getImages().clear();
        addImages(feed, cleanStartKey, cleanEndKey, otherImageKeys, resolvedStartTime, resolvedEndTime);

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

    
}