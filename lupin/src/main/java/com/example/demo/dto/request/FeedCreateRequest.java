package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedCreateRequest {

    @NotBlank(message = "활동 타입은 필수입니다.")
    private String activityType;

    @NotNull(message = "운동 시간은 필수입니다.")
    private Integer duration; // 분 단위 (이미지 메타데이터에서 계산)

    private Double calories;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private List<String> imageUrls; // 이미지 URL 목록
}
