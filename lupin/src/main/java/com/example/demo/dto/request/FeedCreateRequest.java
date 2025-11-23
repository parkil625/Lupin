package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class FeedCreateRequest {

    @NotBlank(message = "활동 타입은 필수입니다.")
    private String activityType;

    private LocalDateTime startedAt;
    private LocalDateTime endedAt;

    private Double calories;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private List<String> images; // S3 이미지 키 목록
}
