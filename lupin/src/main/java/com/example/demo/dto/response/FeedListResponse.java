package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedListResponse {

    private Long id;
    private Long writerId;
    private String authorName;
    private String profileImage;
    private String activityType;
    private Double calories;
    private String content;
    private Long earnedPoints;
    private LocalDateTime createdAt;
    private Integer likesCount;
    private Integer commentsCount;
    private List<String> images;

    /**
     * QueryDSL Projection용 생성자
     */
    public FeedListResponse(Long id, String activityType, Double calories, String content,
                            Long earnedPoints, LocalDateTime createdAt,
                            Long writerId, String authorName,
                            Integer likesCount) {
        this.id = id;
        this.activityType = activityType;
        this.calories = calories;
        this.content = content;
        this.earnedPoints = earnedPoints;
        this.createdAt = createdAt;
        this.writerId = writerId;
        this.authorName = authorName;
        this.profileImage = null;
        this.likesCount = likesCount;
        this.commentsCount = 0; // 별도 조회 필요
        this.images = List.of(); // 별도 조회 필요
    }
}
