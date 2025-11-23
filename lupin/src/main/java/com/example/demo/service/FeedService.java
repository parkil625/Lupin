package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.LotteryTicket;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ImageType;
import com.example.demo.dto.request.FeedCreateRequest;
import com.example.demo.dto.request.FeedUpdateRequest;
import com.example.demo.dto.response.FeedDetailResponse;
import com.example.demo.dto.response.FeedListResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.LotteryTicketRepository;
import com.example.demo.repository.UserPenaltyRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration; // [추가] 시간 차이 계산용
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 피드 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final UserRepository userRepository;
    private final UserPenaltyRepository userPenaltyRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ImageService imageService;
    private final LotteryTicketRepository lotteryTicketRepository;

    private static final long TICKET_PRICE = 30L;

    /**
     * 피드 생성
     */
    @Transactional
    public FeedDetailResponse createFeed(Long userId, FeedCreateRequest request) {
        User user = findUserById(userId);

        // 패널티 확인
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        if (userPenaltyRepository.hasActivePenalty(userId, "FEED", threeDaysAgo)) {
            throw new BusinessException(ErrorCode.PENALTY_ACTIVE, "신고로 인해 3일간 피드 작성이 제한됩니다.");
        }

        // 하루 1회 제한
        if (feedRepository.hasUserPostedToday(userId)) {
            throw new BusinessException(ErrorCode.DAILY_FEED_LIMIT_EXCEEDED);
        }

        // 이미지 필수 검증
        if (request.getImages() == null || request.getImages().size() < 2) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "시작 사진과 끝 사진은 필수입니다.");
        }

        // [수정] 실제 운동 시간 계산 (메타데이터 시간 차이)
        long realDurationMinutes = 0;
        if (request.getStartedAt() != null && request.getEndedAt() != null) {
            // 종료 시간이 시작 시간보다 뒤인지 검증
            if (request.getEndedAt().isBefore(request.getStartedAt())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "종료 시간이 시작 시간보다 빠를 수 없습니다.");
            }
            // 분 단위 차이 계산
            realDurationMinutes = Duration.between(request.getStartedAt(), request.getEndedAt()).toMinutes();
        }

        // [수정] 칼로리 계산 (realDurationMinutes 사용)
        Double calories = request.getCalories();
        if (calories == null || calories == 0) {
            // 시간이 0이면 칼로리도 0
            if (realDurationMinutes > 0) {
                calories = calculateCalories(request.getActivityType(), user, (int) realDurationMinutes);
            } else {
                calories = 0.0;
            }
        }

        // 피드 생성
        Feed feed = Feed.builder()
                .activityType(request.getActivityType())
                .calories(calories)
                .content(request.getContent())
                .build();

        feed.setWriter(user);

        // 이미지 추가
        for (int i = 0; i < request.getImages().size(); i++) {
            String s3Key = request.getImages().get(i);
            ImageType imageType = i == 0 ? ImageType.START :
                    i == 1 ? ImageType.END : ImageType.OTHER;

            FeedImage feedImage = FeedImage.builder()
                    .s3Key(s3Key)
                    .imgType(imageType)
                    .build();

            feed.addImage(feedImage);
        }

        // [수정] 포인트 계산 로직 전면 교체
        // 조건: 메타데이터 시간(realDuration)이 존재해야만 포인트 지급
        Long points = 0L;
        if (realDurationMinutes > 0) {
            points = calculatePoints(request.getActivityType(), (int) realDurationMinutes);
        }

        feed.setEarnedPoints(points);

        Feed savedFeed = feedRepository.save(feed);

        // 포인트 적립 (0점이면 적립 안 함)
        if (points > 0) {
            userService.addPoints(userId, points, "운동 인증", String.valueOf(savedFeed.getId()));
        }

        log.info("피드 생성 완료 - feedId: {}, userId: {}, points: {}, duration: {}분",
                savedFeed.getId(), userId, points, realDurationMinutes);

        return FeedDetailResponse.from(savedFeed);
    }

    // ... (중간 생략: getFeeds, updateFeed, deleteFeed 등은 기존 유지) ...
    // [참고] createFeed 외 다른 메서드는 수정할 필요 없음

    public Page<FeedListResponse> getFeeds(String keyword, String activityType, Long excludeUserId, Long excludeFeedId, Pageable pageable) {
        return feedRepository.searchFeeds(keyword, activityType, excludeUserId, excludeFeedId, pageable);
    }

    public Page<FeedListResponse> getFeedsByUserId(Long userId, Pageable pageable) {
        return feedRepository.findByWriterId(userId, pageable);
    }

    public FeedDetailResponse getFeedDetail(Long feedId) {
        Feed feed = findFeedById(feedId);
        return FeedDetailResponse.from(feed);
    }

    @Transactional
    public FeedDetailResponse updateFeed(Long feedId, Long userId, FeedUpdateRequest request) {
        Feed feed = findFeedById(feedId);
        if (!feed.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_FEED_ACCESS);
        }
        feed.update(request.getContent());
        log.info("피드 수정 완료 - feedId: {}, userId: {}", feedId, userId);
        return FeedDetailResponse.from(feed);
    }

    @Transactional
    public void deleteFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);
        User user = findUserById(userId);

        if (!feed.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_FEED_ACCESS);
        }

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        if (feed.getCreatedAt().isAfter(sevenDaysAgo)) {
            Long pointsToReclaim = feed.getEarnedPoints();
            if (pointsToReclaim != null && pointsToReclaim > 0) {
                reclaimPoints(user, pointsToReclaim);
            }
            int likesCount = feed.getLikesCount();
            if (likesCount > 0) {
                Long currentMonthlyLikes = user.getMonthlyLikes();
                user.setMonthlyLikes(Math.max(0L, currentMonthlyLikes - likesCount));
                log.info("월별 좋아요 회수 - userId: {}, 차감: {}, 잔여: {}", userId, likesCount, user.getMonthlyLikes());
            }
        }

        feed.getImages().forEach(image -> imageService.deleteImage(image.getImageUrl()));
        feedRepository.delete(feed);
        log.info("피드 삭제 완료 - feedId: {}, userId: {}", feedId, userId);
    }

    private void reclaimPoints(User user, Long pointsToReclaim) {
        Long currentPoints = user.getCurrentPoints();
        user.setMonthlyPoints(Math.max(0L, user.getMonthlyPoints() - pointsToReclaim));

        if (currentPoints >= pointsToReclaim) {
            user.setCurrentPoints(currentPoints - pointsToReclaim);
        } else {
            List<LotteryTicket> tickets = lotteryTicketRepository.findByUserId(user.getId());
            if (!tickets.isEmpty()) {
                lotteryTicketRepository.delete(tickets.get(0));
                Long remainingPoints = TICKET_PRICE - pointsToReclaim + currentPoints;
                user.setCurrentPoints(Math.max(0L, remainingPoints));
            } else {
                user.setCurrentPoints(0L);
            }
        }
        userRepository.save(user);
        log.info("포인트 회수 완료 - userId: {}, 회수: {}", user.getId(), pointsToReclaim);
    }

    @Transactional
    public void likeFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);
        User user = findUserById(userId);

        if (feed.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "자신의 피드에는 좋아요를 누를 수 없습니다.");
        }
        if (feedLikeRepository.existsByUserIdAndFeedId(userId, feedId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        FeedLike feedLike = FeedLike.builder().user(user).feed(feed).build();
        feedLikeRepository.save(feedLike);
        feed.getWriter().incrementMonthlyLikes();
        notificationService.createLikeNotification(feed.getWriter().getId(), userId, feedId);
        log.info("피드 좋아요 - feedId: {}, userId: {}", feedId, userId);
    }

    @Transactional
    public void unlikeFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);
        FeedLike feedLike = feedLikeRepository.findByUserIdAndFeedId(userId, feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        feedLikeRepository.delete(feedLike);
        feed.getWriter().decrementMonthlyLikes();
        log.info("피드 좋아요 취소 - feedId: {}, userId: {}", feedId, userId);
    }

    public List<Feed> getPopularFeeds(int limit) {
        return feedRepository.findPopularFeeds(limit);
    }

    public boolean canPostToday(Long userId) {
        return !feedRepository.hasUserPostedToday(userId);
    }

    // [수정] 포인트 계산 로직 변경
    // 1. MET × 분(시간) = 점수 (나누기 10 제거)
    // 2. 소수점 버림 (정수 반환)
    // 3. 최대 30점 제한
    private Long calculatePoints(String activityType, Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes <= 0) {
            return 0L;
        }

        double met = getMET(activityType);

        // 공식: MET × 시간(분)
        double rawPoints = met * durationMinutes;

        // 정수로 반올림
        long points = Math.round(rawPoints);

        // 최대 30점 제한
        return Math.min(points, 30L);
    }

    private Double calculateCalories(String activityType, User user, Integer durationMinutes) {
        double met = getMET(activityType);
        double durationHours = durationMinutes / 60.0;

        int age = LocalDate.now().getYear() - user.getBirthDate().getYear();
        double bmr;
        if ("남성".equals(user.getGender())) {
            bmr = 10 * user.getWeight() + 6.25 * user.getHeight() - 5 * age + 5;
        } else {
            bmr = 10 * user.getWeight() + 6.25 * user.getHeight() - 5 * age - 161;
        }

        double calories = bmr * (met / 24.0) * durationHours;
        return (double) Math.round(calories);
    }

    private double getMET(String activityType) {
        Map<String, Double> metValues = Map.of(
                "러닝", 9.8,
                "걷기", 3.8,
                "자전거", 7.5,
                "수영", 8.0,
                "등산", 6.5,
                "요가", 2.5
        );
        // 기본값은 조금 낮춤
        return metValues.getOrDefault(activityType, 3.0);
    }

    private Feed findFeedById(Long feedId) {
        return feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}