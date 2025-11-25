package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.FeedLike;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.ImageType;
import com.example.demo.domain.enums.PenaltyType;
import com.example.demo.dto.request.FeedCreateRequest;
import com.example.demo.dto.request.FeedUpdateRequest;
import com.example.demo.dto.response.FeedDetailResponse;
import com.example.demo.dto.response.FeedListResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.FeedLikeRepository;
import com.example.demo.repository.FeedRepository;
import com.example.demo.repository.UserPenaltyRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.MetCalculator; // [추가]
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    @Transactional
    public FeedDetailResponse createFeed(Long userId, FeedCreateRequest request) {
        // (FeedCommandService와 로직 동일 - 중복 생략 가능하지만 요청하신 대로 유지한다면 아래와 같이 수정)
        User user = findUserById(userId);

        // ... (검증 로직 생략) ...
        LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
        if (userPenaltyRepository.hasActivePenalty(userId, PenaltyType.FEED, threeDaysAgo)) {
            throw new BusinessException(ErrorCode.PENALTY_ACTIVE, "신고로 인해 3일간 피드 작성이 제한됩니다.");
        }
        if (feedRepository.hasUserPostedToday(userId)) {
            throw new BusinessException(ErrorCode.DAILY_FEED_LIMIT_EXCEEDED);
        }
        if (request.getImages() == null || request.getImages().size() < 2) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "시작 사진과 끝 사진은 필수입니다.");
        }

        long realDurationMinutes = 0;
        if (request.getStartedAt() != null && request.getEndedAt() != null) {
            if (request.getEndedAt().isBefore(request.getStartedAt())) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "종료 시간이 시작 시간보다 빠를 수 없습니다.");
            }
            realDurationMinutes = Duration.between(request.getStartedAt(), request.getEndedAt()).toMinutes();
        }

        Double calories = request.getCalories();
        if (calories == null || calories == 0) {
            if (realDurationMinutes > 0) {
                calories = calculateCalories(request.getActivityType(), user, (int) realDurationMinutes);
            } else {
                calories = 0.0;
            }
        }

        Feed feed = Feed.builder()
                .activityType(request.getActivityType())
                .calories(calories)
                .content(request.getContent())
                .build();

        feed.setWriter(user);

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

        Long points = 0L;
        if (realDurationMinutes > 0) {
            points = calculatePoints(request.getActivityType(), (int) realDurationMinutes);
        }
        feed.setEarnedPoints(points);

        Feed savedFeed = feedRepository.save(feed);

        if (points > 0) {
            userService.addPoints(userId, points, "운동 인증", String.valueOf(savedFeed.getId()));
        }

        log.info("피드 생성 완료 - feedId: {}, userId: {}, points: {}", savedFeed.getId(), userId, points);

        return FeedDetailResponse.from(savedFeed);
    }

    // ... (getFeeds, getFeedDetail, updateFeed, deleteFeed, likeFeed, unlikeFeed 등 기존 메서드 유지) ...
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
            }
        }
        feed.getImages().forEach(image -> imageService.deleteImage(image.getImageUrl()));
        feedRepository.delete(feed);
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
    }

    @Transactional
    public void unlikeFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);
        FeedLike feedLike = feedLikeRepository.findByUserIdAndFeedId(userId, feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND));
        feedLikeRepository.delete(feedLike);
        feed.getWriter().decrementMonthlyLikes();
    }

    public List<Feed> getPopularFeeds(int limit) {
        return feedRepository.findPopularFeeds(limit);
    }

    public boolean canPostToday(Long userId) {
        return !feedRepository.hasUserPostedToday(userId);
    }

    private void reclaimPoints(User user, Long pointsToReclaim) {
        Long currentPoints = user.getCurrentPoints();
        user.setMonthlyPoints(Math.max(0L, user.getMonthlyPoints() - pointsToReclaim));
        user.setCurrentPoints(Math.max(0L, currentPoints - pointsToReclaim));
        userRepository.save(user);
    }

    // [수정] MetCalculator 사용
    private Long calculatePoints(String activityType, Integer duration) {
        double met = MetCalculator.get(activityType); // 호출
        double rawPoints = met * duration;
        long points = Math.round(rawPoints);
        return Math.min(points, 30L);
    }

    private Double calculateCalories(String activityType, User user, Integer durationMinutes) {
        double met = MetCalculator.get(activityType); // 호출
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

    private Feed findFeedById(Long feedId) {
        return feedRepository.findById(feedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEED_NOT_FOUND));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}