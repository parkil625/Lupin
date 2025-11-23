package com.example.demo.dto.response;

import com.example.demo.domain.entity.Feed;
import com.example.demo.domain.entity.FeedImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
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
    private String author;
    private String department;
    private String activity;
    private String content;

    private List<String> images;

    private Integer likes;
    private Integer comments;
    private Long points;
    private LocalDateTime createdAt;

    private Map<String, String> stats;

    public static FeedListResponse from(Feed feed) {
        // [안전장치 1] 작성자 정보가 없을 경우 대비
        Long writerId = (feed.getWriter() != null) ? feed.getWriter().getId() : 0L;
        String author = (feed.getWriter() != null) ? feed.getWriter().getRealName() : "알 수 없음";
        String department = (feed.getWriter() != null) ? feed.getWriter().getDepartment() : "";

        // [안전장치 2] 이미지가 null일 경우 대비
        List<String> imgList = (feed.getImages() != null)
                ? feed.getImages().stream()
                .map(img -> img != null ? img.getImageUrl() : "")
                .filter(url -> url != null && !url.isEmpty())
                .collect(Collectors.toList())
                : Collections.emptyList();

        // [안전장치 3] 칼로리 및 포인트 null 체크
        String calories = (feed.getCalories() != null) ? feed.getCalories() + "kcal" : "0kcal";
        Long points = (feed.getEarnedPoints() != null) ? feed.getEarnedPoints() : 0L;

        return FeedListResponse.builder()
                .id(feed.getId())
                .writerId(writerId)
                .author(author)
                .department(department)
                .activity(feed.getActivityType())
                .content(feed.getContent() != null ? feed.getContent() : "")
                .images(imgList)
                .likes(feed.getLikesCount())
                .comments(feed.getCommentsCount())
                .points(points)
                .createdAt(feed.getCreatedAt())
                .stats(Collections.singletonMap("calories", calories)) // Map.of 대신 호환성 좋은 singletonMap 사용
                .build();
    }
}