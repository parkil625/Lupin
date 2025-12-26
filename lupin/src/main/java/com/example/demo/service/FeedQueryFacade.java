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

/**
 * 피드 조회 파사드 - 조립 로직 분리
 * Controller에서 여러 서비스를 조합하던 로직을 캡슐화
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
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
            
            // [디버그 로그 1] 현재 페이지의 피드 ID 목록 확인
            log.info(">>> [FeedFacade] User ID: {}, 조회 대상 Feed IDs: {}", user.getId(), feedIds);

            // [수정] JPQL 기반 배치 메서드로 정확하게 ID 목록 조회
            List<Long> reportedIds = feedReportRepository.findReportedFeedIdsByReporterId(user.getId(), feedIds);
            
            // [디버그 로그 2] DB에서 찾아낸 신고된 피드 ID 목록 확인
            log.info(">>> [FeedFacade] DB에서 발견된 신고된 Feed IDs: {}", reportedIds);

            reportedFeedIds.addAll(reportedIds);
        }

        final java.util.Set<Long> finalReportedFeedIds = reportedFeedIds;

        List<FeedResponse> content = feeds.getContent().stream()
                .map(feed -> {
                    boolean isLiked = false;
                    if (user != null) {
                        isLiked = feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feed.getId());
                    }
                    
                    boolean isReported = finalReportedFeedIds.contains(feed.getId());
                    
                    // [디버그 로그 3] 최종 DTO 생성 직전 상태 확인 (신고된 건만 로그 출력)
                    if (isReported) {
                         log.info(">>> [FeedFacade] Feed ID {} -> isReported=true 설정됨", feed.getId());
                    }

                    return FeedResponse.from(feed, isLiked, isReported, activeDaysMap.getOrDefault(feed.getWriter().getId(), 0));
                })
                .toList();
        return SliceResponse.of(content, feeds.hasNext(), page, size);
    }
}
