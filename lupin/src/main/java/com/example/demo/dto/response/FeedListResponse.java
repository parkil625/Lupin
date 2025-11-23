package com.example.demo.dto.response;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedListResponse {

    private Long id;
    private Long writerId;
    private String author;        // authorName -> author (프론트엔드 매핑용)
    private String department;    // 부서 정보 추가
    private String activity;      // activityType -> activity (프론트엔드 매핑용)
    private String content;

    // [핵심 수정] imageUrls -> images 로 변경
    private List<String> images;

    private Integer likes;        // likesCount -> likes
    private Integer comments;     // commentsCount -> comments
    private Long points;          // earnedPoints -> points
    private LocalDateTime createdAt;

    // 프론트엔드 'stats' 필드 대응을 위한 칼로리 정보
    private Map<String, String> stats;

    public static FeedListResponse from(Feed feed) {
        return FeedListResponse.builder()
                .id(feed.getId())
                .writerId(feed.getWriter().getId())
                .author(feed.getWriter().getRealName())
                .department(feed.getWriter().getDepartment())
                .activity(feed.getActivityType())
                .content(feed.getContent())
                // [수정] 이미지 리스트 매핑
                .images(feed.getImages().stream()
                        .map(FeedImage::getImageUrl)
                        .collect(Collectors.toList()))
                .likes(feed.getLikesCount())
                .comments(feed.getCommentsCount())
                .points(feed.getEarnedPoints())
                .createdAt(feed.getCreatedAt())
                .stats(Map.of("calories", feed.getCalories() != null ? feed.getCalories() + "kcal" : "0kcal"))
                .build();
    }
}