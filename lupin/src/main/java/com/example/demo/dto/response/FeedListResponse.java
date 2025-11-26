package com.example.demo.dto.response;

import com.example.demo.domain.entity.Feed;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class FeedListResponse {
    private Long id;
    private Long writerId;
    private String writerName;
    private String writerAvatar;
    private String activity;
    private String duration;
    private Map<String, String> stats;
    private String content;
    private Long points;
    private Integer likesCount;
    private Integer commentsCount;
    private List<String> images;
    private LocalDateTime createdAt;
    private boolean isLiked;

    public static FeedListResponse from(Feed feed) {
        Map<String, String> statsMap = new java.util.HashMap<>();
        if (feed.getCalories() != null) {
            statsMap.put("calories", String.valueOf(feed.getCalories()));
        }
        // Parse other stats from JSON if needed, or just use calories for now as it's the main one
        
        return FeedListResponse.builder()
                .id(feed.getId())
                .writerId(feed.getWriter().getId())
                .writerName(feed.getWriter().getName())
                .writerAvatar(feed.getWriter().getAvatar())
                .activity(feed.getActivity())
                .duration(feed.getDuration())
                .stats(statsMap)
                .content(feed.getContent())
                .points(feed.getPoints())
                .likesCount(feed.getLikesCount())
                .commentsCount(feed.getCommentsCount())
                // images conversion logic needed if images are loaded
                .createdAt(feed.getCreatedAt())
                .build();
    }
}
