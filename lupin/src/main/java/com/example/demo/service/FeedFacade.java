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

/**
 * 피드 퍼사드 - 조회(Query)와 명령(Command) 로직 조립
 * Controller에서 여러 서비스를 조합하던 로직을 캡슐화
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedFacade {

    private final FeedReadService feedReadService;
    private final FeedService feedService;
    private final FeedMapper feedMapper;

    /**
     * 홈 피드 목록 조회 (조립 로직 포함)
     */
    public SliceResponse<FeedResponse> getHomeFeeds(User user, int page, int size) {
        Slice<Feed> feeds = feedReadService.getHomeFeeds(user, page, size);
        return feedMapper.toSliceResponse(feeds, user, page, size);
    }

    /**
     * 내 피드 목록 조회 (조립 로직 포함)
     */
    public SliceResponse<FeedResponse> getMyFeeds(User user, int page, int size) {
        Slice<Feed> feeds = feedReadService.getMyFeeds(user, page, size);
        return feedMapper.toSliceResponse(feeds, user, page, size);
    }

    /**
     * 피드 상세 조회
     */
    public FeedResponse getFeedDetail(Long feedId) {
        Feed feed = feedReadService.getFeedDetail(feedId);
        return feedMapper.toResponse(feed);
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
        return feedReadService.canPostToday(user);
    }
}
