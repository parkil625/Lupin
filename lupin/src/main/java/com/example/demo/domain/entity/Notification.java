package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type; // like, comment, reply, comment_like

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false, length = 1)
    @Builder.Default
    private String isRead = "N"; // Y/N

    @Column(name = "ref_id")
    private String refId; // 관련 엔티티 ID (feedId 또는 commentId)

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 비즈니스 로직
    public void markAsRead() {
        this.isRead = "Y";
    }

    public void setUser(User user) {
        this.user = user;
        if (!user.getNotifications().contains(this)) {
            user.getNotifications().add(this);
        }
    }
}
