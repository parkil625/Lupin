package com.example.demo.dto.response;

import com.example.demo.domain.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 댓글 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentResponse {

    private Long id;
    private String content;
    private Long writerId;
    private String writerName;
    private String writerProfileImage;
    private Long feedId;
    private Long parentId;
    private Integer replyCount;
    private List<CommentResponse> replies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity -> Response DTO 변환 (답글 포함)
     */
    public static CommentResponse from(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .writerId(comment.getWriter().getId())
                .writerName(comment.getWriter().getName())
                .writerProfileImage(comment.getWriter().getProfileImage())
                .feedId(comment.getFeed().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replyCount(comment.getReplies() != null ? comment.getReplies().size() : 0)
                .replies(comment.getReplies() != null ?
                        comment.getReplies().stream()
                                .map(CommentResponse::from)
                                .collect(Collectors.toList()) : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * Entity -> Response DTO 변환 (답글 제외)
     */
    public static CommentResponse fromWithoutReplies(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .writerId(comment.getWriter().getId())
                .writerName(comment.getWriter().getName())
                .writerProfileImage(comment.getWriter().getProfileImage())
                .feedId(comment.getFeed().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .replyCount(comment.getReplies() != null ? comment.getReplies().size() : 0)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
