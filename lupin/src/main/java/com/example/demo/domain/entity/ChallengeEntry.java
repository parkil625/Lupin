package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "challenge_entry",
        uniqueConstraints = @UniqueConstraint(columnNames = {"challenge_id", "user_id"}),
        indexes = {@Index(name="idx_challenge_id", columnList = "challenge_id")}
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChallengeEntry{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Version
    private Long version;

    // 편의 메서드
    public static ChallengeEntry of(Challenge challenge, User user, LocalDateTime now) {
        challenge.join(now);

        return ChallengeEntry.builder()
                .challenge(challenge)
                .user(user)
                .joinedAt(now)
                .build();
    }

}
