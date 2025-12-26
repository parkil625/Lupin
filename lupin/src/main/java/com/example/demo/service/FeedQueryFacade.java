package com.example.demo.service;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.command.FeedCreateCommand;
import com.example.demo.dto.command.FeedUpdateCommand;
import com.example.demo.dto.response.FeedResponse;
import com.example.demo.dto.response.SliceResponse;
import com.example.demo.mapper.FeedMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 피드 조회 파사드 - 조립 로직 분리
 * Controller에서 여러 서비스를 조합하던 로직을 캡슐화
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedQueryFacade {

    private final FeedService feedService;
    private final FeedMapper feedMapper;
    private final com.example.demo.repository.FeedLikeRepository feedLikeRepository;
    private final com.example.demo.repository.FeedReportRepository feedReportRepository;

    /**
     * 홈 피드 목록 조회 (조립 로직 포함)
     */
    public SliceResponse<FeedResponse> getHomeFeeds(User user, int page, int size) {
        Slice<Feed> feeds = feedService.getHomeFeeds(user, page, size);
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
     */
    private SliceResponse<FeedResponse> toSliceResponse(Slice<Feed> feeds, User user, int page, int size) {
        Map<Long, Integer> activeDaysMap = feedService.getActiveDaysMap(feeds.getContent());
        
        java.util.Set<Long> reportedFeedIds = new java.util.HashSet<>();

        if (user != null && !feeds.isEmpty()) {
            List<Long> feedIds = feeds.getContent().stream().map(Feed::getId).toList();
            // [수정] Native Query 기반 배치 메서드로 정확하게 ID 목록 조회 (목록 화면 오류 해결)
            List<Long> reportedIds = feedReportRepository.findReportedFeedIdsByReporterId(user.getId(), feedIds);
            reportedFeedIds.addAll(reportedIds);
        }

        final java.util.Set<Long> finalReportedFeedIds = reportedFeedIds;

        List<FeedResponse> content = feeds.getContent().stream()
                .map(feed -> {
                    boolean isLiked = false;
                    // 좋아요는 기존 로직 유지 (필요 시 위와 같이 최적화 권장)
                    if (user != null) {
                        isLiked = feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feed.getId());
                    }
                    
                    // 신고 여부는 메모리(Set)에서 O(1)로 확인
                    boolean isReported = finalReportedFeedIds.contains(feed.getId());

                    return FeedResponse.from(feed, isLiked, isReported, activeDaysMap.getOrDefault(feed.getWriter().getId(), 0));
                })
                .toList();
        return SliceResponse.of(content, feeds.hasNext(), page, size);
    }
}
