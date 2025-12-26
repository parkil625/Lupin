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
     */
    private SliceResponse<FeedResponse> toSliceResponse(Slice<Feed> feeds, User user, int page, int size) {
        Map<Long, Integer> activeDaysMap = feedService.getActiveDaysMap(feeds.getContent());

        // [변경] 불확실한 배치 조회(Bulk Fetch) 제거 -> 좋아요와 동일한 Loop 방식 사용

        List<FeedResponse> content = feeds.getContent().stream()
                .map(feed -> {
                    boolean isLiked = false;
                    boolean isReported = false;

                    if (user != null) {
                        // 1. 좋아요 확인 (기존 로직)
                        isLiked = feedLikeRepository.existsByUserIdAndFeedId(user.getId(), feed.getId());
                        
                        // 2. 신고 확인 (좋아요 벤치마킹: 똑같은 방식으로 확인)
                        isReported = feedReportRepository.existsByReporter_IdAndFeed_Id(user.getId(), feed.getId());
                    }

                    return FeedResponse.from(feed, isLiked, isReported, activeDaysMap.getOrDefault(feed.getWriter().getId(), 0));
                })
                .toList();
        return SliceResponse.of(content, feeds.hasNext(), page, size);
    }
}
