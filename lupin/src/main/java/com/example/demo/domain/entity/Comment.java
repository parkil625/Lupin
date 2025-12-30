package com.example.demo.domain.entity;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.BatchSize; // [추가] BatchSize 임포트

@Entity
@Table(name = "comments")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE comments SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feed_id", nullable = false)
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // [N+1 해결] 대댓글 조회 최적화 - 부모 댓글 로딩 시 자식 댓글들을 배치로 조회
    @BatchSize(size = 100)
    @OneToMany(mappedBy = "parent")
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    // [수정] 댓글 삭제 시 신고 내역은 유지하기 위해 cascade = CascadeType.REMOVE 제거
    @OneToMany(mappedBy = "comment")
    @Builder.Default
    private List<CommentReport> commentReports = new ArrayList<>();

    // [수정] 좋아요 수 (반정규화) - Builder.Default 추가
    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private int likeCount = 0;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 소유권 검증 (Rich Domain Model)
     * @throws BusinessException COMMENT_NOT_OWNER if not the owner
     */
    public void validateOwner(User user) {
        if (!this.writer.getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_OWNER);
        }
    }
}