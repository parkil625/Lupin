package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "feeds", indexes = {
    @Index(name = "idx_writer_created", columnList = "writer_id, created_at DESC")
})
@EntityListeners(AuditingEntityListener.class)
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
    @BatchSize(size = 10)
    private Set<FeedImage> images = new HashSet<>();

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

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Feed(User writer, String activity, String content, long points, int calories) {
        this.writer = writer;
        this.activity = activity;
        this.content = content;
        this.points = points;
        this.calories = calories;
        this.images = new HashSet<>();
        this.likeCount = 0;
        this.commentCount = 0;
    }

    public void update(String content, String activity) {
        this.content = content;
        this.activity = activity;
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

    // 썸네일 URL 설정
    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
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
