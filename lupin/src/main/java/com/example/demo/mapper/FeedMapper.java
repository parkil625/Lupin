package com.example.demo.mapper;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.response.FeedResponse;
import com.example.demo.service.FeedLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Feed 엔티티를 FeedResponse DTO로 변환하는 Mapper
 */
@Component
@RequiredArgsConstructor
public class FeedMapper {

    private final FeedLikeService feedLikeService;

    /**
     * Feed 엔티티를 FeedResponse로 변환 (기본)
     */
    public FeedResponse toResponse(Feed feed) {
        return FeedResponse.from(feed);
    }

    /**
     * Feed 엔티티를 FeedResponse로 변환 (좋아요 여부 포함)
     */
    public FeedResponse toResponse(Feed feed, User currentUser) {
        boolean isLiked = currentUser != null && feedLikeService.isLiked(currentUser.getId(), feed.getId());
        return FeedResponse.from(feed, isLiked);
    }

    /**
     * Feed 엔티티를 FeedResponse로 변환 (좋아요 여부 + activeDays 포함)
     */
    public FeedResponse toResponse(Feed feed, User currentUser, Map<Long, Integer> activeDaysMap) {
        boolean isLiked = currentUser != null && feedLikeService.isLiked(currentUser.getId(), feed.getId());
        Integer activeDays = activeDaysMap.getOrDefault(feed.getWriter().getId(), 0);
        return FeedResponse.from(feed, isLiked, activeDays);
    }
}
