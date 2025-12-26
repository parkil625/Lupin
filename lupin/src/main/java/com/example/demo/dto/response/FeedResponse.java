package com.example.demo.dto.response;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    // [삭제] private Boolean isReported;  <-- 기존 중복 필드 삭제

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // [추가] 이미지 촬영 시간 정보 (프론트엔드 수정 시 사용)
    private List<LocalDateTime> imageCapturedAt;

    // [수정] boolean(기본형) -> Boolean(참조형)으로 변경
    // 이유: 기본형 boolean은 Lombok이 Getter를 isReported()로 만들어, Jackson이 "reported"로 키 이름을 잘라버림.
    // Boolean 참조형은 getIsReported()로 만들어져, JSON 키 "isReported"가 정상 유지됨. (isLiked 필드와 동일 원리)
    @JsonProperty("isReported")
    private Boolean isReported;

    /**
     * [최적화] Feed 엔티티의 반정규화 필드 사용 - DB 조회 없음
     */
    public static FeedResponse from(Feed feed) {
        return from(feed, false);
    }

    public static FeedResponse from(Feed feed, boolean isLiked) {
        return from(feed, isLiked, false, null);
    }

    public static FeedResponse from(Feed feed, boolean isLiked, Integer activeDays) {
        return from(feed, isLiked, false, activeDays);
    }

    public static FeedResponse from(Feed feed, Long viewerId) {
        boolean isReported = false;
        if (viewerId != null && feed.getFeedReports() != null) {
            isReported = feed.getFeedReports().stream()
                    .anyMatch(report -> report.getReporter().getId().equals(viewerId));
        }
        return from(feed, false, isReported, null);
    }

    public static FeedResponse from(Feed feed, Long viewerId, boolean isLiked) {
        boolean isReported = false;
        if (viewerId != null && feed.getFeedReports() != null) {
            isReported = feed.getFeedReports().stream()
                    .anyMatch(report -> report.getReporter().getId().equals(viewerId));
        }
        return from(feed, isLiked, isReported, null);
    }

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
                .imageCapturedAt(feed.getImages() != null ? feed.getImages().stream()
                        .sorted(Comparator.comparingInt(FeedImage::getSortOrder))
                        .map(FeedImage::getCapturedAt)
                        .collect(Collectors.toList()) : Collections.emptyList())
                .likes((long) feed.getLikeCount())
                .comments((long) feed.getCommentCount())
                .isLiked(isLiked)
                .isReported(isReported)
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }

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
                .isReported(false)
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }

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
                .isReported(false)
                .createdAt(feed.getCreatedAt())
                .updatedAt(feed.getUpdatedAt())
                .build();
    }
}