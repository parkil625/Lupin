package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type; // challenge, appointment, like, comment

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read", nullable = false, length = 1)
    @Builder.Default
    private String isRead = "N"; // Y/N

    @Column(name = "ref_type", length = 50)
    private String refType; // FEED, COMMENT, CHAT 등

    @Column(name = "ref_id")
    private String refId; // 관련 엔티티 ID

    @Column(name = "related_id")
    @Deprecated
    private Long relatedId; // 하위 호환성을 위해 유지

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
