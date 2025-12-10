package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class FeedRequest {

    @NotBlank(message = "활동은 필수입니다")
    private String activity;

    @Size(max = 50000, message = "피드 내용이 너무 깁니다")
    private String content;

    private List<String> images = new ArrayList<>();

    @Builder
    public FeedRequest(String activity, String content, List<String> images) {
        this.activity = activity;
        this.content = content;
        this.images = images != null ? images : new ArrayList<>();
    }
}
