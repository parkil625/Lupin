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
                           Optional<LocalDateTime> startTimeOpt, Optional<LocalDateTime> endTimeOpt) {
        Feed feed = feedRepository.findByIdWithWriter(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));

        feed.validateOwner(user);

        // [변경 감지] 기존 데이터 백업
        String oldActivity = feed.getActivity();
        long oldPoints = feed.getPoints();
        
        // 기존 시작/끝 이미지 키 찾기 (순서나 타입 기반)
        String oldStartKey = feed.getImages().stream()
                .filter(img -> img.getImgType() == ImageType.START)
                .map(FeedImage::getS3Key).findFirst().orElse("");
        String oldEndKey = feed.getImages().stream()
                .filter(img -> img.getImgType() == ImageType.END)
                .map(FeedImage::getS3Key).findFirst().orElse("");

        // 1. 기존 이미지들의 시간 정보 백업 (Key -> Time)
        Map<String, LocalDateTime> oldTimeMap = feed.getImages().stream()
                .filter(img -> img.getCapturedAt() != null)
                .collect(Collectors.toMap(FeedImage::getS3Key, FeedImage::getCapturedAt, (a, b) -> a));

        // 정보 업데이트 (활동 내용 반영)
        feed.update(content, activity);

        // 2. 시간 결정: 새 추출 값이 있으면 사용, 없으면(기존 유지) 기존 DB 값 사용
        LocalDateTime resolvedStartTime = startTimeOpt.orElse(oldTimeMap.get(startImageKey));
        LocalDateTime resolvedEndTime = endTimeOpt.orElse(oldTimeMap.get(endImageKey));

        // 시간 정보 유실 방지 (수정 시 이미지는 그대로인데 시간이 null이 되는 경우 방지)
        if (resolvedStartTime == null || resolvedEndTime == null) {
            log.error("Feed update failed: Time metadata lost. startKey={}, endKey={}", startImageKey, endImageKey);
            throw new BusinessException(ErrorCode.FEED_IMAGES_REQUIRED); // 적절한 에러 코드로 대체 가능
        }

        // 3. 포인트/점수 재계산 여부 판단
        // 로직: 이미지가 하나라도 바뀌었거나, 운동 종류(Activity)가 바뀌었을 때만 재계산
        boolean isImageChanged = !startImageKey.equals(oldStartKey) || !endImageKey.equals(oldEndKey);
        boolean isActivityChanged = !activity.equals(oldActivity);

        if (isImageChanged || isActivityChanged) {
            log.info("Recalculating score for feed {}. ImageChanged={}, ActivityChanged={}", feedId, isImageChanged, isActivityChanged);
            
            // 점수 계산 (WorkoutScoreService에 위임)
            WorkoutScoreService.WorkoutResult workoutResult =
                    workoutScoreService.validateAndCalculate(activity, 
                            Optional.ofNullable(resolvedStartTime), 
                            Optional.ofNullable(resolvedEndTime), 
                            feed.getCreatedAt().toLocalDate());

            if (!workoutResult.valid()) {
                log.warn("Feed update - Workout time validation failed. Activity={}, Start={}, End={}", 
                        activity, resolvedStartTime, resolvedEndTime);
                // 시간이 있는데 계산 실패라면(예: 시간이 역전됨) 에러 처리 or 0점 처리 (정책에 따라 결정)
                // 여기서는 0점으로 덮어씌워지지 않도록 방어하려면 예외를 던지는 게 낫습니다.
            }

            // 피드 점수 업데이트
            feed.updateScore((long) workoutResult.score(), workoutResult.calories());
            
            // 포인트 조정 (PointService에 위임) - 기존 포인트와 비교하여 차액만큼 처리
            pointService.adjustFeedPoints(user, oldPoints, workoutResult.score());
        } else {
            log.info("Skipping score calculation for feed {}. No changes in images or activity.", feedId);
            // 포인트 변동 없음 (adjustFeedPoints 호출 안 함)
        }

        feed.updateThumbnail(startImageKey);

        // 4. 이미지 정보 갱신 (시간 정보 유지)
        // 재계산을 안 했더라도 resolvedTime(기존 시간)을 다시 넣어줘야 DB에 유지됨
        feed.getImages().clear();
        addImages(feed, startImageKey, endImageKey, otherImageKeys, resolvedStartTime, resolvedEndTime);

        feedRepository.flush();
        return feed;
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