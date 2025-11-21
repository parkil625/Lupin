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
    private Boolean isEdited;          // 수정 여부 (연필 아이콘 표시용)
    private LocalDateTime displayDate; // 표시할 날짜 (수정됐으면 updatedAt, 아니면 createdAt)

    /**
     * Entity -> Response DTO 변환 (답글 포함)
     */
    public static CommentResponse from(Comment comment) {
        boolean edited = comment.getUpdatedAt() != null &&
                        !comment.getUpdatedAt().equals(comment.getCreatedAt());
        LocalDateTime display = edited ? comment.getUpdatedAt() : comment.getCreatedAt();

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
                .isEdited(edited)
                .displayDate(display)
                .build();
    }

    /**
     * Entity -> Response DTO 변환 (답글 제외)
     */
    public static CommentResponse fromWithoutReplies(Comment comment) {
        boolean edited = comment.getUpdatedAt() != null &&
                        !comment.getUpdatedAt().equals(comment.getCreatedAt());
        LocalDateTime display = edited ? comment.getUpdatedAt() : comment.getCreatedAt();

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
                .isEdited(edited)
                .displayDate(display)
                .build();
    }
}
