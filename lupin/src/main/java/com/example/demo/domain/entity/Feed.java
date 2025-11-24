package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "feeds", indexes = {
    @Index(name = "idx_feed_writer", columnList = "writerId"),
    @Index(name = "idx_feed_created", columnList = "createdAt DESC"),
    @Index(name = "idx_feed_activity", columnList = "activityType")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Feed extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // writerId는 인덱스/쿼리용으로 유지하고, writer 관계도 추가
    @Column(nullable = false, insertable = false, updatable = false)
    private Long writerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writerId", nullable = false)
    private User writer;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @Column
    private Double calories;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "earned_points", nullable = false)
    @Builder.Default
    private Long earnedPoints = 0L;

    // 카운터 캐싱 - 조회 성능 최적화
    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private Integer likesCount = 0;

    @Column(name = "comments_count", nullable = false)
    @Builder.Default
    private Integer commentsCount = 0;

    // 이미지 목록
    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<FeedImage> images = new java.util.ArrayList<>();

    // 동시성 제어 - 카운터 업데이트 경쟁 방지
    @Version
    private Long version;

    // 비즈니스 로직
    public void setWriter(User writer) {
        this.writer = writer;
    }

    public void addImage(FeedImage image) {
        images.add(image);
        image.setFeed(this);
    }

    public void update(String content) {
        if (content != null) {
            this.content = content;
        }
    }

    public void setEarnedPoints(Long earnedPoints) {
        this.earnedPoints = earnedPoints;
    }

    public void incrementLikesCount() {
        this.likesCount++;
    }

    public void decrementLikesCount() {
        if (this.likesCount > 0) {
            this.likesCount--;
        }
    }

    public void incrementCommentsCount() {
        this.commentsCount++;
    }

    public void decrementCommentsCount() {
        if (this.commentsCount > 0) {
            this.commentsCount--;
        }
    }
}
