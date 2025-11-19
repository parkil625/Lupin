package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feed")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Feed extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column
    private Integer duration; // 분 단위

    @Column
    private Double calories;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "stats_json", columnDefinition = "TEXT")
    private String statsJson; // JSON 형식의 통계 데이터

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User writer;

    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<FeedImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FeedLike> likes = new ArrayList<>();

    // 편의 메서드
    public void setWriter(User user) {
        this.writer = user;
        if (!user.getFeeds().contains(this)) {
            user.getFeeds().add(this);
        }
    }

    public void addImage(FeedImage image) {
        this.images.add(image);
        image.setFeed(this);
    }

    public void addComment(Comment comment) {
        this.comments.add(comment);
        comment.setFeed(this);
    }

    public int getLikesCount() {
        return likes != null ? likes.size() : 0;
    }

    public int getCommentsCount() {
        return comments != null ? comments.size() : 0;
    }
}
