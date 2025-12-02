package com.example.demo.dto.response;

import com.example.demo.domain.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {

    private Long id;
    private Long feedId;
    private String content;
    private String writerName;
    private String writerAvatar;
    private Long writerId;
    private LocalDateTime createdAt;
    private Long parentId;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .feedId(comment.getFeed().getId())
                .content(comment.getContent())
                .writerName(comment.getWriter().getName())
                .writerAvatar(comment.getWriter().getAvatar())
                .writerId(comment.getWriter().getId())
                .createdAt(comment.getCreatedAt())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .build();
    }
}
