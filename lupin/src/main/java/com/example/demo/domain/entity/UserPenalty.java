package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 패널티 엔티티
 * 신고로 인한 콘텐츠 삭제 시 생성
 */
@Entity
@Table(name = "user_penalty",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "penalty_type"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "penalty_type", nullable = false, length = 20)
    private String penaltyType; // FEED, COMMENT

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 패널티 갱신 (새 삭제 발생 시)
     */
    public void refresh() {
        this.createdAt = LocalDateTime.now();
    }
}
