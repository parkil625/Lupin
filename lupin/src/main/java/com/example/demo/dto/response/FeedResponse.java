package com.example.demo.dto.response;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
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
    private String writerDepartment;
    private Integer writerActiveDays;
    private Long points;
    private Integer calories;
    private List<String> images;
    private Long likes;
    private Long comments;
    private Boolean isLiked;
    private Boolean isReported;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * [최적화] Feed 엔티티의 반정규화 필드 사용 - DB 조회 없음
     */
    public static FeedResponse from(Feed feed) {
        return from(feed, false);
    }

    /**
     * [최적화] Feed 엔티티의 반정규화 필드 사용 - DB 조회 없음
     */
    public static FeedResponse from(Feed feed, boolean isLiked) {
        return from(feed, isLiked, false, null);
    }

    /**
     * [최적화] Feed 엔티티의 반정규화 필드 사용 + activeDays 포함
     */
    public static FeedResponse from(Feed feed, boolean isLiked, Integer activeDays) {
        return from(feed, isLiked, false, activeDays);
    }

    /**
     * [최적화] Feed 엔티티의 반정규화 필드 사용 + activeDays + isReported 포함
     */
    public static FeedResponse from(Feed feed, boolean isLiked, boolean isReported, Integer activeDays) {
        return FeedResponse.builder()
                .id(feed.getId())
                .activity(feed.getActivity())
                .content(feed.getContent())
                .writerName(feed.getWriter().getName())
                .writerId(feed.getWriter().getId())
                .writerAvatar(feed.getWriter().getAvatar())
                .writerDepartment(feed.getWriter().getDepartment())
                .writerActiveDays(activeDays)
                .points(feed.getPoints())
                .calories(feed.getCalories())
                .images(feed.getImages() != null ? feed.getImages().stream()
                        .sorted(Comparator.comparingInt(FeedImage::getSortOrder))
                        .map(FeedImage::getS3Key)
                        .collect(Collectors.toList()) : Collections.emptyList())
                .likes((long) feed.getLikeCount())
                .comments((long) feed.getCommentCount())
                .isLiked(isLiked)
                .isReported(isReported)
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }

    /**
     * @deprecated 반정규화 필드 사용으로 대체 - from(Feed, boolean) 사용 권장
     */
    @Deprecated
    public static FeedResponse from(Feed feed, long likeCount, long commentCount) {
        return FeedResponse.builder()
                .id(feed.getId())
                .activity(feed.getActivity())
                .content(feed.getContent())
                .writerName(feed.getWriter().getName())
                .writerId(feed.getWriter().getId())
                .writerAvatar(feed.getWriter().getAvatar())
                .writerDepartment(feed.getWriter().getDepartment())
                .points(feed.getPoints())
                .calories(feed.getCalories())
                .images(feed.getImages() != null ? feed.getImages().stream()
                        .sorted(Comparator.comparingInt(FeedImage::getSortOrder))
                        .map(FeedImage::getS3Key)
                        .collect(Collectors.toList()) : Collections.emptyList())
                .likes(likeCount)
                .comments(commentCount)
                .isLiked(false)
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }

    /**
     * @deprecated 반정규화 필드 사용으로 대체 - from(Feed, boolean) 사용 권장
     */
    @Deprecated
    public static FeedResponse from(Feed feed, long likeCount, long commentCount, boolean isLiked) {
        return FeedResponse.builder()
                .id(feed.getId())
                .activity(feed.getActivity())
                .content(feed.getContent())
                .writerName(feed.getWriter().getName())
                .writerId(feed.getWriter().getId())
                .writerAvatar(feed.getWriter().getAvatar())
                .writerDepartment(feed.getWriter().getDepartment())
                .points(feed.getPoints())
                .calories(feed.getCalories())
                .images(feed.getImages() != null ? feed.getImages().stream()
                        .sorted(Comparator.comparingInt(FeedImage::getSortOrder))
                        .map(FeedImage::getS3Key)
                        .collect(Collectors.toList()) : Collections.emptyList())
                .likes(likeCount)
                .comments(commentCount)
                .isLiked(isLiked)
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }
}
