package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comment_feed", columnList = "feedId"),
    @Index(name = "idx_comment_writer", columnList = "writerId"),
    @Index(name = "idx_comment_parent", columnList = "parentId"),
    @Index(name = "idx_comment_created", columnList = "createdAt DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, insertable = false, updatable = false)
    private Long writerId;

    @Column(nullable = false, insertable = false, updatable = false)
    private Long feedId;

    @Column(insertable = false, updatable = false)
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writerId", nullable = false)
    private User writer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedId", nullable = false)
    @Setter
    private Feed feed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parentId")
    private Comment parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Comment> replies = new ArrayList<>();

    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CommentLike> likes = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 카운터 캐싱
    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private Integer likesCount = 0;

    @Column(name = "replies_count", nullable = false)
    @Builder.Default
    private Integer repliesCount = 0;

    // 동시성 제어
    @Version
    private Long version;

    // 비즈니스 로직
    public void updateContent(String content) {
        this.content = content;
    }

    public boolean isReply() {
        return this.parentId != null;
    }

    public void addReply(Comment reply) {
        this.replies.add(reply);
        this.incrementRepliesCount();
    }

    public void incrementLikesCount() {
        this.likesCount++;
    }

    public void decrementLikesCount() {
        if (this.likesCount > 0) {
            this.likesCount--;
        }
    }

    public void incrementRepliesCount() {
        this.repliesCount++;
    }

    public void decrementRepliesCount() {
        if (this.repliesCount > 0) {
            this.repliesCount--;
        }
    }
}
