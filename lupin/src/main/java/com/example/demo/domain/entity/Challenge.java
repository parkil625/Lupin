package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ChallengeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "challenge")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Challenge extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "opens_at", nullable = false)
    private LocalDateTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalDateTime closesAt;

    @Column(name = "max_winners")
    private Integer maxWinners;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChallengeStatus status = ChallengeStatus.SCHEDULED;

    // 연관관계
    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChallengeEntry> entries = new ArrayList<>();

    // 비즈니스 로직
    public void open() {
        if (LocalDateTime.now().isBefore(opensAt)) {
            throw new IllegalStateException("아직 챌린지 시작 시간이 아닙니다.");
        }
        this.status = ChallengeStatus.ACTIVE;
    }

    public void close() {
        this.status = ChallengeStatus.CLOSED;
    }

    public boolean canJoin() {
        return status == ChallengeStatus.ACTIVE
            && LocalDateTime.now().isBefore(closesAt);
    }
}
