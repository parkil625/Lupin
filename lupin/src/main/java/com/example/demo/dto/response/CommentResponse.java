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
    private String writerDepartment;
    private Integer writerActiveDays;
    private Long writerId;
    private LocalDateTime createdAt;
    private Long parentId;
    private Long likeCount;
    private Boolean isLiked;

    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .feedId(comment.getFeed().getId())
                .content(comment.getContent())
                .writerName(comment.getWriter().getName())
                .writerAvatar(comment.getWriter().getAvatar())
                .writerDepartment(comment.getWriter().getDepartment())
                .writerId(comment.getWriter().getId())
                .createdAt(comment.getCreatedAt())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .likeCount(0L)
                .isLiked(false)
                .build();
    }

    public static CommentResponse from(Comment comment, long likeCount, boolean isLiked) {
        return from(comment, likeCount, isLiked, null);
    }

    public static CommentResponse from(Comment comment, long likeCount, boolean isLiked, Integer activeDays) {
        return CommentResponse.builder()
                .id(comment.getId())
                .feedId(comment.getFeed().getId())
                .content(comment.getContent())
                .writerName(comment.getWriter().getName())
                .writerAvatar(comment.getWriter().getAvatar())
                .writerDepartment(comment.getWriter().getDepartment())
                .writerActiveDays(activeDays)
                .writerId(comment.getWriter().getId())
                .createdAt(comment.getCreatedAt())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .likeCount(likeCount)
                .isLiked(isLiked)
                .build();
    }
}
