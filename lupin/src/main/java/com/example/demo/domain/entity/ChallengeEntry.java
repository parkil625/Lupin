package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "challenge_entry",
    uniqueConstraints = @UniqueConstraint(columnNames = {"challenge_id", "user_id"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChallengeEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "joined_at", nullable = false)
    @Builder.Default
    private LocalDateTime joinedAt = LocalDateTime.now();

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 편의 메서드
    public static ChallengeEntry of(Challenge challenge, User user) {
        if (!challenge.canJoin()) {
            throw new IllegalStateException("참가할 수 없는 챌린지입니다.");
        }
        return ChallengeEntry.builder()
                .challenge(challenge)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .build();
    }
}
