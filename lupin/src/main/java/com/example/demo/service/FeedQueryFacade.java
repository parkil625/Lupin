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
            // 기존에 존재하는 배치 조회 메서드를 활용하여 신고 여부 확인 (리스트로 감싸서 호출)
            isReported = !feedReportRepository.findReportedFeedIdsByReporterId(user.getId(), List.of(feedId)).isEmpty();
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
        
        // [성능 최적화] N+1 방지: ID 목록 추출 후 한 번의 쿼리로 조회
        java.util.Set<Long> likedFeedIds = java.util.Collections.emptySet();
        java.util.Set<Long> reportedFeedIds = java.util.Collections.emptySet();

        if (user != null && !feeds.isEmpty()) {
            List<Long> feedIds = feeds.getContent().stream().map(Feed::getId).toList();
            // 좋아요 목록 Batch 조회 (repository에 해당 메서드가 없다면 추가 필요, 유사하게 구현)
            // likedFeedIds = new java.util.HashSet<>(feedLikeRepository.findLikedFeedIdsByUserId(user.getId(), feedIds)); 
            // 위 메서드가 없다면 기존처럼 루프 돌거나 repository 추가 필요. 여기선 reported만 최적화 예시
            
            // 신고 목록 Batch 조회
            reportedFeedIds = new java.util.HashSet<>(feedReportRepository.findReportedFeedIdsByReporterId(user.getId(), feedIds));
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
