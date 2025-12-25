package com.example.demo.domain.entity;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;
import com.example.demo.domain.enums.ImageType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "feeds", indexes = {
    @Index(name = "idx_writer_created", columnList = "writer_id, created_at DESC")
})
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE feeds SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"writer", "images"})
public class Feed {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id", nullable = false)
    private User writer;

    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<FeedImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "feed", cascade = CascadeType.REMOVE)
    private List<FeedReport> feedReports = new ArrayList<>();

    @Column(nullable = false, length = 50)
    private String activity;

    @Column(nullable = false)
    private int calories;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private long points;

    // 반정규화 필드 - 성능 최적화
    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    private int commentCount = 0;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT") // TEXT로 변경하여 제한 해제
    private String thumbnailUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Feed(User writer, String activity, String content, long points, int calories) {
        this.writer = writer;
        this.activity = activity;
        this.content = content;
        this.points = points;
        this.calories = calories;
        this.images = new ArrayList<>();
        this.likeCount = 0;
        this.commentCount = 0;
    }

    public void update(String content, String activity) {
        this.content = content;
        this.activity = activity;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateScore(long points, int calories) {
        this.points = points;
        this.calories = calories;
    }

    // 좋아요 카운트 증감 메서드
    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    // 댓글 카운트 증감 메서드
    public void incrementCommentCount() {
        this.commentCount++;
    }

    public void decrementCommentCount() {
        if (this.commentCount > 0) {
            this.commentCount--;
        }
    }

    public void updateThumbnail(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public Optional<FeedImage> getStartImage() {
        return this.images.stream()
                .filter(img -> img.getImgType() == ImageType.START)
                .findFirst();
    }

    public Optional<FeedImage> getEndImage() {
        return this.images.stream()
                .filter(img -> img.getImgType() == ImageType.END)
                .findFirst();
    }

    /**
     * 소유권 검증 (Rich Domain Model)
     * @throws BusinessException FEED_NOT_OWNER if not the owner
     */
    public void validateOwner(User user) {
        if (!Objects.equals(this.writer.getId(), user.getId())) {
            throw new BusinessException(ErrorCode.FEED_NOT_OWNER);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Feed feed = (Feed) o;
        return id != null && Objects.equals(id, feed.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
