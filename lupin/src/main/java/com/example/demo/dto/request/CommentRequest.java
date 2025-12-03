package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentRequest {

    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(max = 500, message = "댓글은 500자 이하로 작성해주세요")
    private String content;

    private Long parentId;

    public CommentRequest(String content) {
        this.content = content;
    }

    public CommentRequest(String content, Long parentId) {
        this.content = content;
        this.parentId = parentId;
    }
}
