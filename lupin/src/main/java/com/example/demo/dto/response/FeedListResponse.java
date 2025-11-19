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
    private String authorName;
    private String activityType;
    private Integer duration;
    private Double calories;
    private String content;
    private String statsJson;
    private LocalDateTime createdAt;
    private Integer likesCount;
    private Integer commentsCount;
    private List<String> images;
}
