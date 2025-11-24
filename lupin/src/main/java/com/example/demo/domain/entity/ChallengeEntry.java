package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "challenge_entries", uniqueConstraints = {
    @UniqueConstraint(name = "uk_entry_challenge_user", columnNames = {"challengeId", "userId"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChallengeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long challengeId;

    @Column(nullable = false)
    private Long userId;

    private LocalDateTime joinedAt;

    /**
     * 팩토리 메서드 - 엔티티로부터 생성
     */
    public static ChallengeEntry of(Challenge challenge, User user, LocalDateTime joinedAt) {
        return ChallengeEntry.builder()
                .challengeId(challenge.getId())
                .userId(user.getId())
                .joinedAt(joinedAt)
                .build();
    }

    /**
     * 팩토리 메서드 - ID로부터 생성
     */
    public static ChallengeEntry of(Long challengeId, Long userId, LocalDateTime joinedAt) {
        return ChallengeEntry.builder()
                .challengeId(challengeId)
                .userId(userId)
                .joinedAt(joinedAt)
                .build();
    }
}
