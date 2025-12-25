package com.example.demo.dto.response;

import com.example.demo.domain.entity.Comment;
import com.example.demo.domain.entity.CommentReport;
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
    private LocalDateTime updatedAt;
    private Boolean isReported;

    // [추가할 코드]
    /**
     * [권장] viewerId를 받아 신고 여부를 정확히 확인
     */
    public static CommentResponse from(Comment comment, Long viewerId) {
        boolean isReported = false;
        if (viewerId != null && comment.getCommentReports() != null) {
            isReported = comment.getCommentReports().stream()
                    .anyMatch(report -> report.getReporter().getId().equals(viewerId));
        }
        // 기존 빌더 로직 활용 또는 직접 빌드 (여기선 직접 빌드)
        return CommentResponse.builder()
                .id(comment.getId())
                .feedId(comment.getFeed().getId())
                .content(comment.getContent())
                .writerName(comment.getWriter().getName())
                .writerAvatar(comment.getWriter().getAvatar())
                .writerDepartment(comment.getWriter().getDepartment())
                .writerId(comment.getWriter().getId())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .likeCount(0L) // 필요시 수정
                .isLiked(false)
                .isReported(isReported)
                .build();
    }

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
                .updatedAt(comment.getUpdatedAt())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
            .likeCount(0L)
            .isLiked(false)
            .isReported(false)
            .build();
    }

    public static CommentResponse from(Comment comment, long likeCount, boolean isLiked) {
        return from(comment, likeCount, isLiked, false, null);
    }

    public static CommentResponse from(Comment comment, long likeCount, boolean isLiked, boolean isReported) {
        return from(comment, likeCount, isLiked, isReported, null);
    }

    public static CommentResponse from(Comment comment, long likeCount, boolean isLiked, boolean isReported, Integer activeDays) {
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
                .updatedAt(comment.getUpdatedAt())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
            .likeCount(likeCount)
            .isLiked(isLiked)
            .isReported(isReported)
            .build();
    }
}
