package com.example.demo.mapper;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.User;
import com.example.demo.dto.response.FeedResponse;
import com.example.demo.dto.response.SliceResponse;
import com.example.demo.service.FeedLikeService;
import com.example.demo.service.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Feed 엔티티를 FeedResponse DTO로 변환하는 Mapper
 */
@Component
@RequiredArgsConstructor
public class FeedMapper {

    private final FeedLikeService feedLikeService;
    private final UserReadService userReadService;

    public FeedResponse toResponse(Feed feed) {
        return FeedResponse.from(feed);
    }

    public FeedResponse toResponse(Feed feed, User currentUser) {
        boolean isLiked = currentUser != null && feedLikeService.isLiked(currentUser.getId(), feed.getId());
        return FeedResponse.from(feed, isLiked);
    }

    public FeedResponse toResponse(Feed feed, User currentUser, Map<Long, Integer> activeDaysMap) {
        boolean isLiked = currentUser != null && feedLikeService.isLiked(currentUser.getId(), feed.getId());
        Integer activeDays = activeDaysMap.getOrDefault(feed.getWriter().getId(), 0);
        return FeedResponse.from(feed, isLiked, activeDays);
    }

    public SliceResponse<FeedResponse> toSliceResponse(Slice<Feed> feeds, User user, int page, int size) {
        List<Long> writerIds = feeds.getContent().stream()
                .map(feed -> feed.getWriter().getId())
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Integer> activeDaysMap = userReadService.getActiveDaysMap(writerIds);

        List<FeedResponse> content = feeds.getContent().stream()
                .map(feed -> toResponse(feed, user, activeDaysMap))
                .toList();
        return SliceResponse.of(content, feeds.hasNext(), page, size);
    }
}
