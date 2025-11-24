package com.example.demo.domain.entity;

import com.example.demo.domain.enums.PenaltyType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_penalties",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_penalty", columnNames = {"userId", "penaltyType"})
    },
    indexes = {
        @Index(name = "idx_penalty_user", columnList = "userId"),
        @Index(name = "idx_penalty_expires", columnList = "expiresAt")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserPenalty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, insertable = false, updatable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "penalty_type", nullable = false, length = 20)
    private PenaltyType penaltyType;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "penalty_count", nullable = false)
    @Builder.Default
    private Integer penaltyCount = 1;

    public void refresh() {
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusDays(3);
        this.penaltyCount++;
    }

    public boolean isActive() {
        return LocalDateTime.now().isBefore(this.expiresAt);
    }

    public static UserPenalty create(Long userId, PenaltyType penaltyType) {
        return UserPenalty.builder()
                .userId(userId)
                .penaltyType(penaltyType)
                .expiresAt(LocalDateTime.now().plusDays(3))
                .build();
    }
}
