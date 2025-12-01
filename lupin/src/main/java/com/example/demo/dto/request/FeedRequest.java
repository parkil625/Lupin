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

    @Size(max = 1000, message = "피드 내용은 1000자 이하로 작성해주세요")
    private String content;

    private List<String> imageUrls = new ArrayList<>();

    @Builder
    public FeedRequest(String activity, String content, List<String> imageUrls) {
        this.activity = activity;
        this.content = content;
        this.imageUrls = imageUrls != null ? imageUrls : new ArrayList<>();
    }
}
