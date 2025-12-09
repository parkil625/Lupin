package com.example.demo.dto.response;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class FeedResponse {

    private Long id;
    private String activity;
    private String content;
    private String writerName;
    private Long writerId;
    private String writerAvatar;
    private Long points;
    private Integer calories;
    private List<String> images;
    private Long likes;
    private Long comments;
    private Boolean isLiked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FeedResponse from(Feed feed, long likeCount, long commentCount) {
        return FeedResponse.builder()
                .id(feed.getId())
                .activity(feed.getActivity())
                .content(feed.getContent())
                .writerName(feed.getWriter().getName())
                .writerId(feed.getWriter().getId())
                .writerAvatar(feed.getWriter().getAvatar())
                .points(feed.getPoints())
                .calories(feed.getCalories())
                .images(feed.getImages().stream()
                        .sorted(Comparator.comparingInt(FeedImage::getSortOrder))
                        .map(FeedImage::getS3Key)
                        .collect(Collectors.toList()))
                .likes(likeCount)
                .comments(commentCount)
                .isLiked(false)
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }

    public static FeedResponse from(Feed feed, long likeCount, long commentCount, boolean isLiked) {
        return FeedResponse.builder()
                .id(feed.getId())
                .activity(feed.getActivity())
                .content(feed.getContent())
                .writerName(feed.getWriter().getName())
                .writerId(feed.getWriter().getId())
                .writerAvatar(feed.getWriter().getAvatar())
                .points(feed.getPoints())
                .calories(feed.getCalories())
                .images(feed.getImages().stream()
                        .sorted(Comparator.comparingInt(FeedImage::getSortOrder))
                        .map(FeedImage::getS3Key)
                        .collect(Collectors.toList()))
                .likes(likeCount)
                .comments(commentCount)
                .isLiked(isLiked)
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }
}
