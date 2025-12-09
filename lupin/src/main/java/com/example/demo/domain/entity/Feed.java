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

    @Version
    private Long version;

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
        this.images = new HashSet<>(); // Builder로 생성 시에도 초기화
        this.version = 0L; // @Version 필드 초기화 (테스트에서 ReflectionTestUtils 사용 시 필요)
    }

    public void update(String content, String activity) {
        this.content = content;
        this.activity = activity;
    }

    public void updateScore(long points, int calories) {
        this.points = points;
        this.calories = calories;
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
