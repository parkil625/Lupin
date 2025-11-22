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
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.LotteryTicketRepository;
import com.example.demo.repository.UserPenaltyRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 피드 Command 서비스 (쓰기 전용)
 * CQRS 패턴 - 데이터 변경 작업 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FeedCommandService {

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
    public Long createFeed(Long userId, FeedCreateRequest request) {
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

        // 이미지 검증
        if (request.getImages() == null || request.getImages().size() < 2) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "시작 사진과 끝 사진은 필수입니다.");
        }

        // 칼로리 계산
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

        // 포인트 계산
        Long points = calculatePoints(request.getActivityType(), request.getDuration());
        feed.setEarnedPoints(points);

        Feed savedFeed = feedRepository.save(feed);

        // 포인트 적립
        userService.addPoints(userId, points, "운동 인증", String.valueOf(savedFeed.getId()));

        log.info("피드 생성 완료 - feedId: {}, userId: {}, points: {}", savedFeed.getId(), userId, points);

        return savedFeed.getId();
    }

    /**
     * 피드 수정
     */
    public void updateFeed(Long feedId, Long userId, FeedUpdateRequest request) {
        Feed feed = findFeedById(feedId);

        if (!feed.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_FEED_ACCESS);
        }

        feed.update(request.getContent());

        log.info("피드 수정 완료 - feedId: {}, userId: {}", feedId, userId);
    }

    /**
     * 피드 삭제
     */
    public void deleteFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);
        User user = findUserById(userId);

        if (!feed.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_FEED_ACCESS);
        }

        // 7일 이내 삭제 시 포인트/좋아요 회수
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
            }
        }

        // S3 이미지 삭제
        feed.getImages().forEach(image -> {
            imageService.deleteImage(image.getImageUrl());
        });

        feedRepository.delete(feed);

        log.info("피드 삭제 완료 - feedId: {}, userId: {}", feedId, userId);
    }

    /**
     * 피드 좋아요
     */
    public void likeFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);
        User user = findUserById(userId);

        if (feed.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "자신의 피드에는 좋아요를 누를 수 없습니다.");
        }

        if (feedLikeRepository.existsByUserIdAndFeedId(userId, feedId)) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        FeedLike feedLike = FeedLike.builder()
                .user(user)
                .feed(feed)
                .build();

        feedLikeRepository.save(feedLike);
        feed.getWriter().incrementMonthlyLikes();

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
    public void unlikeFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);
        FeedLike feedLike = feedLikeRepository.findByUserIdAndFeedId(userId, feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));

        feedLikeRepository.delete(feedLike);
        feed.getWriter().decrementMonthlyLikes();

        log.info("피드 좋아요 취소 - feedId: {}, userId: {}", feedId, userId);
    }

    // === Private Methods ===

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
    }

    private Long calculatePoints(String activityType, Integer duration) {
        double met = getMET(activityType);
        double points = (met * duration) / 10.0;
        return Math.min(Math.round(points), 30L);
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
        return metValues.getOrDefault(activityType, 5.0);
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
