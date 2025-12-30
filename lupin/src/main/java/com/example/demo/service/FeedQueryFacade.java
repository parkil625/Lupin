package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.command.FeedCreateCommand;
import com.example.demo.dto.command.FeedUpdateCommand;
import com.example.demo.dto.response.FeedResponse;
import com.example.demo.dto.response.SliceResponse;
import com.example.demo.mapper.FeedMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/**
 * 피드 조회 파사드 - 조립 로직 분리
 * Controller에서 여러 서비스를 조합하던 로직을 캡슐화
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = false)
@Slf4j
public class FeedQueryFacade {

    private final FeedService feedService;
    private final FeedMapper feedMapper;
    private final com.example.demo.repository.FeedLikeRepository feedLikeRepository;
    private final com.example.demo.repository.FeedReportRepository feedReportRepository;

    /**
     * 홈 피드 목록 조회 (조립 로직 포함)
     */
    public SliceResponse<FeedResponse> getHomeFeeds(User user, int page, int size, String search) {
        // [수정] search 파라미터 전달
        Slice<Feed> feeds = feedService.getHomeFeeds(user, page, size, search);
        return toSliceResponse(feeds, user, page, size);
    }

    /**
     * 내 피드 목록 조회 (조립 로직 포함)
     */
    public SliceResponse<FeedResponse> getMyFeeds(User user, int page, int size) {
        Slice<Feed> feeds = feedService.getMyFeeds(user, page, size);
        return toSliceResponse(feeds, user, page, size);
    }

    /**
     * 피드 상세 조회 (상태 조립 포함)
     */
    public FeedResponse getFeedDetail(User user, Long feedId) {
        Feed feed = feedService.getFeedDetail(feedId);

        boolean isLiked = false;
        boolean isReported = false;

        if (user != null) {
            isLiked = feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feedId);
            
            isReported = feedReportRepository.countByReporterIdAndFeedId(user.getId(), feedId) > 0;
        }

        Map<Long, Integer> activeDaysMap = feedService.getActiveDaysMap(List.of(feed));

        return FeedResponse.from(feed, isLiked, isReported, activeDaysMap.getOrDefault(feed.getWriter().getId(), 0));
    }

    /**
     * 피드 생성 (Command 패턴)
     */
    @Transactional
    public FeedResponse createFeed(FeedCreateCommand command) {
        Feed feed = feedService.createFeed(command);
        return feedMapper.toResponse(feed);
    }

    /**
     * 피드 수정 (Command 패턴)
     */
    @Transactional
    public FeedResponse updateFeed(FeedUpdateCommand command) {
        // [추가] 디버깅 로그: 프론트엔드에서 보낸 변경 플래그와 시간 정보 확인
        log.info("=== [Feed Update Debug] ID: {}, Changed: {}, Start: {}, End: {} ===", 
                command.feedId(), command.imagesChanged(), command.startAt(), command.endAt());

        Feed feed = feedService.updateFeed(command);
        return feedMapper.toResponse(feed);
    }

    /**
     * 피드 삭제
     */
    @Transactional
    public void deleteFeed(User user, Long feedId) {
        feedService.deleteFeed(user, feedId);
    }

    /**
     * 오늘 포스팅 가능 여부
     */
    public boolean canPostToday(User user) {
        return feedService.canPostToday(user);
    }

    /**
     * Slice를 SliceResponse로 변환 (조립 로직)
     * [최적화] N+1 문제 해결: 반복문 내 쿼리 제거 -> Bulk 조회 적용
     */
    private SliceResponse<FeedResponse> toSliceResponse(Slice<Feed> feeds, User user, int page, int size) {
        Map<Long, Integer> activeDaysMap = feedService.getActiveDaysMap(feeds.getContent());

        Set<Long> likedFeedIds = new HashSet<>();
        Set<Long> reportedFeedIds = new HashSet<>();

        // [최적화] 로그인한 유저가 있다면, 현재 페이지의 피드들에 대해 좋아요/신고 여부를 한 번에 조회
        if (user != null && !feeds.getContent().isEmpty()) {
            List<Long> feedIds = feeds.getContent().stream()
                    .map(Feed::getId)
                    .toList();

            // 1. 좋아요 Bulk 조회
            List<Long> likedList = feedLikeRepository.findFeedIdsByUserIdAndFeedIdIn(user.getId(), feedIds);
            likedFeedIds.addAll(likedList);

            // 2. 신고 Bulk 조회
            List<Long> reportedList = feedReportRepository.findFeedIdsByReporterIdAndFeedIdIn(user.getId(), feedIds);
            reportedFeedIds.addAll(reportedList);

            log.info("=== [Feed Facade] Bulk Fetch - Feeds: {}, Liked: {}, Reported: {} ===", 
                    feedIds.size(), likedFeedIds.size(), reportedFeedIds.size());
        }

        List<FeedResponse> content = feeds.getContent().stream()
                .map(feed -> {
                    boolean isLiked = false;
                    boolean isReported = false;

                    if (user != null) {
                        // 메모리 상에서 O(1) 조회
                        isLiked = likedFeedIds.contains(feed.getId());
                        isReported = reportedFeedIds.contains(feed.getId());
                    }

                    return FeedResponse.from(feed, isLiked, isReported, activeDaysMap.getOrDefault(feed.getWriter().getId(), 0));
                })
                .toList();

        return SliceResponse.of(content, feeds.hasNext(), page, size);
    }
}
