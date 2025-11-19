package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedListResponse {

    private Long id;
    private String authorName;
    private String activityType;
    private Integer duration;
    private String content;
    private LocalDateTime createdAt;
    private Integer likesCount;
    private Integer commentsCount;
}
