package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "point_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PointLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(name = "ref_id", length = 100)
    private String refId; // 참조 ID (Feed ID, Challenge ID 등)

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 편의 메서드
    public void setUser(User user) {
        this.user = user;
        if (!user.getPointLogs().contains(this)) {
            user.getPointLogs().add(this);
        }
    }
}
