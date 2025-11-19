package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.example.demo.domain.entity.FeedLike;
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
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * 피드 생성
     */
    @Transactional
    public FeedDetailResponse createFeed(Long userId, FeedCreateRequest request) {
        User user = findUserById(userId);

        // 피드 생성
        Feed feed = Feed.builder()
                .activityType(request.getActivityType())
                .duration(request.getDuration())
                .calories(request.getCalories())
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

        // 포인트 적립 (운동 시간에 따라)
        Long points = calculatePoints(request.getDuration());
        userService.addPoints(userId, points, "운동 인증", String.valueOf(savedFeed.getId()));

        log.info("피드 생성 완료 - feedId: {}, userId: {}, points: {}", savedFeed.getId(), userId, points);

        return FeedDetailResponse.from(savedFeed);
    }

    /**
     * 피드 목록 조회 (검색, 페이징)
     */
    public Page<FeedListResponse> getFeeds(String keyword, String activityType, Pageable pageable) {
        return feedRepository.searchFeeds(keyword, activityType, pageable);
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

        // 필드 업데이트 (빌더 패턴 대신 setter 사용 또는 엔티티에 업데이트 메서드 추가)
        // 여기서는 간단히 새로운 Feed를 만들지 않고 기존 엔티티 수정
        // Feed 엔티티에 update 메서드 추가 권장

        log.info("피드 수정 완료 - feedId: {}, userId: {}", feedId, userId);

        return FeedDetailResponse.from(feed);
    }

    /**
     * 피드 삭제
     */
    @Transactional
    public void deleteFeed(Long feedId, Long userId) {
        Feed feed = findFeedById(feedId);

        // 작성자 확인
        if (!feed.getWriter().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_FEED_ACCESS);
        }

        feedRepository.delete(feed);

        log.info("피드 삭제 완료 - feedId: {}, userId: {}", feedId, userId);
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
     * 포인트 계산 (5분당 5점)
     */
    private Long calculatePoints(Integer duration) {
        return (long) ((duration / 5) * 5);
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
