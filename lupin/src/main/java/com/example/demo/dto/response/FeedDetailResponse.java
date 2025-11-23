package com.example.demo.dto.response;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedDetailResponse {

    private Long id;
    private Long writerId;
    private String authorName;
    private String activityType;
    private Double calories;
    private String content;
    private List<String> imageUrls;
    private Integer likesCount;
    private Integer commentsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FeedDetailResponse from(Feed feed) {
        return FeedDetailResponse.builder()
                .id(feed.getId())
                .writerId(feed.getWriter().getId())
                .authorName(feed.getWriter().getRealName())
                .activityType(feed.getActivityType())
                .calories(feed.getCalories())
                .content(feed.getContent())
                .imageUrls(feed.getImages().stream()
                        .map(FeedImage::getImageUrl)
                        .collect(Collectors.toList()))
                .likesCount(feed.getLikesCount())
                .commentsCount(feed.getCommentsCount())
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }
}
