package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.LotteryTicket;
import com.example.demo.domain.entity.PointLog;
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
import com.example.demo.repository.PointLogRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final UserService userService;
    private final NotificationService notificationService;
    private final ImageService imageService;
    private final PointLogRepository pointLogRepository;
    private final LotteryTicketRepository lotteryTicketRepository;

    // 추첨권 가격 (30포인트 = 1장)
    private static final long TICKET_PRICE = 30L;

    /**
     * 피드 생성
     */
    @Transactional
    public FeedDetailResponse createFeed(Long userId, FeedCreateRequest request) {
        User user = findUserById(userId);

        // 하루 1회 피드 작성 제한 확인
        if (feedRepository.hasUserPostedToday(userId)) {
            throw new BusinessException(ErrorCode.DAILY_FEED_LIMIT_EXCEEDED);
        }

        // 칼로리 계산 (request에 없으면 자동 계산)
        Double calories = request.getCalories();
        if (calories == null || calories == 0) {
            calories = calculateCalories(request.getActivityType(), user, request.getDuration());
        }

        // 피드 생성
        Feed feed = Feed.builder()
                .activityType(request.getActivityType())
                .duration(request.getDuration())
                .calories(calories)
                .content(request.getContent())
                .statsJson(request.getStatsJson())
                .startedAt(LocalDateTime.now())
                .build();

        feed.setWriter(user);

        // 이미지 추가
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                String imageUrl = request.getImageUrls().get(i);
                ImageType imageType = i == 0 ? ImageType.START :
                                    i == 1 ? ImageType.END : ImageType.OTHER;

                FeedImage feedImage = FeedImage.builder()
                        .imageUrl(imageUrl)
                        .imgType(imageType)
                        .sortOrder(i)
                        .takenAt(LocalDateTime.now())
                        .build();

                feed.addImage(feedImage);
            }
        }

        Feed savedFeed = feedRepository.save(feed);

        // 포인트 적립 (MET × 시간 기반)
        Long points = calculatePoints(request.getActivityType(), request.getDuration());
        userService.addPoints(userId, points, "운동 인증", String.valueOf(savedFeed.getId()));

        log.info("피드 생성 완료 - feedId: {}, userId: {}, points: {}", savedFeed.getId(), userId, points);

        return FeedDetailResponse.from(savedFeed);
    }

    /**
     * 피드 목록 조회 (검색, 페이징)
     */
    public Page<FeedListResponse> getFeeds(String keyword, String activityType, Long excludeUserId, Pageable pageable) {
        return feedRepository.searchFeeds(keyword, activityType, excludeUserId, pageable);
    }

    /**
     * 특정 사용자의 피드 조회
     */
    public Page<FeedListResponse> getFeedsByUserId(Long userId, Pageable pageable) {
        return feedRepository.findByWriterId(userId, pageable);
    }

    /**
     * 피드 상세 조회
     */
    public FeedDetailResponse getFeedDetail(Long feedId) {
        Feed feed = findFeedById(feedId);
        return FeedDetailResponse.from(feed);
    }

    /**
     * 피드 수정
     */
    @Transactional
    public FeedDetailResponse updateFeed(Long feedId, Long userId, FeedUpdateRequest request) {
        Feed feed = findFeedById(feedId);

        // 작성자 확인
        if (!feed.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_FEED_ACCESS);
        }

        // 변경 감지를 통한 업데이트
        feed.update(request.getContent(), request.getStatsJson());

        log.info("피드 수정 완료 - feedId: {}, userId: {}", feedId, userId);

        return FeedDetailResponse.from(feed);
    }

    /**
     * 피드 삭제
     */
    @Transactional
    public void deleteFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);
        User user = findUserById(userId);

        // 작성자 확인
        if (!feed.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_FEED_ACCESS);
        }

        // 실제 부여된 포인트 조회
        List<PointLog> pointLogs = pointLogRepository.findByUserIdAndRefIdAndReason(
                userId, String.valueOf(feedId), "운동 인증");

        Long pointsToReclaim = pointLogs.stream()
                .mapToLong(PointLog::getAmount)
                .sum();

        // 포인트 회수 (3단계 로직)
        if (pointsToReclaim > 0) {
            reclaimPoints(user, pointsToReclaim, feedId);
        }

        // S3 이미지 삭제
        feed.getImages().forEach(image -> {
            imageService.deleteImage(image.getImageUrl());
        });

        feedRepository.delete(feed);

        log.info("피드 삭제 완료 - feedId: {}, userId: {}, 회수 포인트: {}, 삭제 이미지: {}개",
                feedId, userId, pointsToReclaim, feed.getImages().size());
    }

    /**
     * 포인트 회수 (3단계 로직)
     * 1. currentPoints에서 차감
     * 2. 부족하면 추첨권 판매 (TICKET_PRICE 포인트로 환산)
     * 3. 그래도 부족하면 totalPoints에서 차감
     */
    private void reclaimPoints(User user, Long pointsToReclaim, Long feedId) {
        Long currentPoints = user.getCurrentPoints();
        Long remainingDebt = pointsToReclaim;

        // 1단계: currentPoints에서 차감
        if (currentPoints >= remainingDebt) {
            user.setCurrentPoints(currentPoints - remainingDebt);
            remainingDebt = 0L;
        } else {
            user.setCurrentPoints(0L);
            remainingDebt -= currentPoints;
        }

        // 2단계: 추첨권 판매로 충당 (미사용 추첨권만)
        if (remainingDebt > 0) {
            List<LotteryTicket> tickets = lotteryTicketRepository.findByUserIdAndIsUsed(user.getId(), "N");

            for (LotteryTicket ticket : tickets) {
                if (remainingDebt <= 0) break;

                // 미사용 추첨권 삭제
                lotteryTicketRepository.delete(ticket);

                // 추첨권 가치에서 빚 차감
                if (TICKET_PRICE >= remainingDebt) {
                    // 거스름돈 발생
                    Long change = TICKET_PRICE - remainingDebt;
                    user.setCurrentPoints(user.getCurrentPoints() + change);
                    remainingDebt = 0L;
                } else {
                    remainingDebt -= TICKET_PRICE;
                }
            }
        }

        // 3단계: 그래도 부족하면 totalPoints에서 차감
        if (remainingDebt > 0) {
            Long totalPoints = user.getTotalPoints();
            user.setTotalPoints(Math.max(0L, totalPoints - remainingDebt));
        }

        // 포인트 차감 로그 기록
        PointLog deductLog = PointLog.builder()
                .amount(-pointsToReclaim)
                .reason("피드 삭제")
                .refId(String.valueOf(feedId))
                .build();
        deductLog.setUser(user);
        pointLogRepository.save(deductLog);

        userRepository.save(user);

        log.info("포인트 회수 완료 - userId: {}, 회수: {}, 현재포인트: {}, 총포인트: {}",
                user.getId(), pointsToReclaim, user.getCurrentPoints(), user.getTotalPoints());
    }

    /**
     * 피드 좋아요
     */
    @Transactional
    public void likeFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);
        User user = findUserById(userId);

        // 이미 좋아요를 누른 경우
        if (feedLikeRepository.existsByUserIdAndFeedId(userId, feedId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        FeedLike feedLike = FeedLike.builder()
                .user(user)
                .feed(feed)
                .build();

        feedLikeRepository.save(feedLike);

        // 피드 작성자에게 좋아요 알림 (자신의 피드가 아닌 경우)
        if (!feed.getWriter().getId().equals(userId)) {
            notificationService.createLikeNotification(
                feed.getWriter().getId(),
                userId,
                feedId
            );
        }

        log.info("피드 좋아요 - feedId: {}, userId: {}", feedId, userId);
    }

    /**
     * 피드 좋아요 취소
     */
    @Transactional
    public void unlikeFeed(Long feedId, Long userId) {
        FeedLike feedLike = feedLikeRepository.findByUserIdAndFeedId(userId, feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        feedLikeRepository.delete(feedLike);

        log.info("피드 좋아요 취소 - feedId: {}, userId: {}", feedId, userId);
    }

    /**
     * 인기 피드 조회
     */
    public List<Feed> getPopularFeeds(int limit) {
        return feedRepository.findPopularFeeds(limit);
    }

    /**
     * 오늘 피드 작성 가능 여부 확인
     */
    public boolean canPostToday(Long userId) {
        return !feedRepository.hasUserPostedToday(userId);
    }

    /**
     * 포인트 계산 (MET × 시간 기반, 최대 30점)
     * - 성별/체중 무관하게 운동 강도와 시간으로만 계산
     */
    private Long calculatePoints(String activityType, Integer duration) {
        double met = getMET(activityType);
        // points = (MET × duration) / 10, 최대 30점
        double points = (met * duration) / 10.0;
        return Math.min(Math.round(points), 30L);
    }

    /**
     * 칼로리 계산 (Mifflin-St Jeor BMR 공식)
     * - 성별, 키, 체중, 나이를 고려한 정확한 칼로리 계산
     */
    private Double calculateCalories(String activityType, User user, Integer durationMinutes) {
        double met = getMET(activityType);
        double durationHours = durationMinutes / 60.0;

        // BMR 계산 (Mifflin-St Jeor 공식)
        int age = LocalDate.now().getYear() - user.getBirthDate().getYear();
        double bmr;
        if ("남성".equals(user.getGender())) {
            // 남성: BMR = 10 × weight + 6.25 × height - 5 × age + 5
            bmr = 10 * user.getWeight() + 6.25 * user.getHeight() - 5 * age + 5;
        } else {
            // 여성: BMR = 10 × weight + 6.25 × height - 5 × age - 161
            bmr = 10 * user.getWeight() + 6.25 * user.getHeight() - 5 * age - 161;
        }

        // Calories = BMR × (MET / 24) × duration (hours)
        double calories = bmr * (met / 24.0) * durationHours;
        return (double) Math.round(calories);
    }

    /**
     * MET (Metabolic Equivalent of Task) 값 조회
     */
    private double getMET(String activityType) {
        Map<String, Double> metValues = Map.of(
            "러닝", 9.8,
            "걷기", 3.8,
            "자전거", 7.5,
            "수영", 8.0,
            "등산", 6.5,
            "요가", 2.5
        );
        return metValues.getOrDefault(activityType, 5.0);
    }

    /**
     * ID로 피드 조회 (내부 메서드)
     */
    private Feed findFeedById(Long feedId) {
        return feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
    }

    /**
     * ID로 사용자 조회 (내부 메서드)
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
