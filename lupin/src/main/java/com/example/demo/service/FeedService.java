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
    private final LotteryTicketRepository lotteryTicketRepository;
    private final com.example.demo.repository.ReportRepository reportRepository;

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

        // 3일 내 신고 기록이 있는 경우 피드 작성 불가
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        if (reportRepository.hasRecentReport(userId, threeDaysAgo)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "신고 기록으로 인해 3일간 피드 작성이 제한됩니다.");
        }

        // 시작/끝 이미지 필수 검증 (최소 2장)
        if (request.getImageUrls() == null || request.getImageUrls().size() < 2) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "시작 사진과 끝 사진은 필수입니다.");
        }

        // 칼로리 계산 (request에 없으면 자동 계산)
        Double calories = request.getCalories();
        if (calories == null || calories == 0) {
            calories = calculateCalories(request.getActivityType(), user, request.getDuration());
        }

        // 피드 생성
        Feed feed = Feed.builder()
                .activityType(request.getActivityType())
                .calories(calories)
                .content(request.getContent())
                .build();

        feed.setWriter(user);

        // 이미지 추가 (START, END 필수)
        for (int i = 0; i < request.getImageUrls().size(); i++) {
            String s3Key = request.getImageUrls().get(i);
            ImageType imageType = i == 0 ? ImageType.START :
                                i == 1 ? ImageType.END : ImageType.OTHER;

            FeedImage feedImage = FeedImage.builder()
                    .s3Key(s3Key)
                    .imgType(imageType)
                    .build();

            feed.addImage(feedImage);
        }

        // 포인트 계산 (MET × 시간 기반)
        Long points = calculatePoints(request.getActivityType(), request.getDuration());
        feed.setEarnedPoints(points);

        Feed savedFeed = feedRepository.save(feed);

        // 포인트 적립
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
        feed.update(request.getContent());

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

        // 피드 생성일시가 7일 이내일 경우에만 포인트 및 좋아요 회수
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        if (feed.getCreatedAt().isAfter(sevenDaysAgo)) {
            // 포인트 회수
            Long pointsToReclaim = feed.getEarnedPoints();
            if (pointsToReclaim != null && pointsToReclaim > 0) {
                reclaimPoints(user, pointsToReclaim);
            }

            // 월별 좋아요 회수 (해당 피드의 좋아요 수만큼 차감)
            int likesCount = feed.getLikesCount();
            if (likesCount > 0) {
                Long currentMonthlyLikes = user.getMonthlyLikes();
                user.setMonthlyLikes(Math.max(0L, currentMonthlyLikes - likesCount));
                log.info("월별 좋아요 회수 - userId: {}, 차감: {}, 잔여: {}",
                        userId, likesCount, user.getMonthlyLikes());
            }
        }

        // S3 이미지 삭제
        feed.getImages().forEach(image -> {
            imageService.deleteImage(image.getImageUrl());
        });

        feedRepository.delete(feed);

        log.info("피드 삭제 완료 - feedId: {}, userId: {}, 삭제 이미지: {}개",
                feedId, userId, feed.getImages().size());
    }

    /**
     * 포인트 회수
     * 1. 현재 점수 >= 회수할 점수: 현재 점수에서 차감
     * 2. 현재 점수 < 회수할 점수 && 추첨권 있음: 추첨권 1장 소멸, 남는 점수는 현재 점수에 더함
     * 3. 현재 점수 < 회수할 점수 && 추첨권 없음: 현재 점수 0
     */
    private void reclaimPoints(User user, Long pointsToReclaim) {
        Long currentPoints = user.getCurrentPoints();

        // 월별 점수 차감 (0 미만이면 0)
        user.setMonthlyPoints(Math.max(0L, user.getMonthlyPoints() - pointsToReclaim));

        if (currentPoints >= pointsToReclaim) {
            // 현재 점수가 충분한 경우
            user.setCurrentPoints(currentPoints - pointsToReclaim);
        } else {
            // 현재 점수가 부족한 경우
            List<LotteryTicket> tickets = lotteryTicketRepository.findByUserId(user.getId());

            if (!tickets.isEmpty()) {
                // 추첨권 1장 소멸
                lotteryTicketRepository.delete(tickets.get(0));
                // 남는 점수 = 30 - 회수할 점수 + 현재 점수
                Long remainingPoints = TICKET_PRICE - pointsToReclaim + currentPoints;
                user.setCurrentPoints(Math.max(0L, remainingPoints));
            } else {
                // 추첨권도 없으면 현재 점수 0
                user.setCurrentPoints(0L);
            }
        }

        userRepository.save(user);

        log.info("포인트 회수 완료 - userId: {}, 회수: {}, 잔여포인트: {}, 월간포인트: {}",
                user.getId(), pointsToReclaim, user.getCurrentPoints(), user.getMonthlyPoints());
    }

    /**
     * 피드 좋아요
     */
    @Transactional
    public void likeFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);
        User user = findUserById(userId);

        // 자신의 피드에는 좋아요 불가
        if (feed.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "자신의 피드에는 좋아요를 누를 수 없습니다.");
        }

        // 이미 좋아요를 누른 경우
        if (feedLikeRepository.existsByUserIdAndFeedId(userId, feedId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        FeedLike feedLike = FeedLike.builder()
                .user(user)
                .feed(feed)
                .build();

        feedLikeRepository.save(feedLike);

        // 피드 작성자의 월별 좋아요 증가
        feed.getWriter().incrementMonthlyLikes();

        // 피드 작성자에게 좋아요 알림
        notificationService.createLikeNotification(
            feed.getWriter().getId(),
            userId,
            feedId
        );

        log.info("피드 좋아요 - feedId: {}, userId: {}", feedId, userId);
    }

    /**
     * 피드 좋아요 취소
     */
    @Transactional
    public void unlikeFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);
        FeedLike feedLike = feedLikeRepository.findByUserIdAndFeedId(userId, feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        feedLikeRepository.delete(feedLike);

        // 피드 작성자의 월별 좋아요 감소
        feed.getWriter().decrementMonthlyLikes();

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
