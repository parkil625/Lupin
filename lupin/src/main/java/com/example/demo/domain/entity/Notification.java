package com.example.demo.domain.entity;

import com.example.demo.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "ref_id")
    private String refId;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "actor_profile_image")
    private String actorProfileImage;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void markAsRead() {
        this.isRead = true;
    }

    // [추가] 알림 뭉치기/갱신을 위한 비즈니스 메서드
    public void updateForAggregation(String title, String actorProfileImage) {
        this.title = title;
        this.actorProfileImage = actorProfileImage;
        this.createdAt = LocalDateTime.now(); // 시간을 현재로 갱신하여 최상단으로 끌어올림
    }

    // [추가] 시간만 갱신 (단순 덮어쓰기용)
    public void refreshCreatedAt() {
        this.createdAt = LocalDateTime.now();
    }
}
