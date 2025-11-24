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
public class Challenge{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "opens_at", nullable = false)
    private LocalDateTime opensAt;

    @Column(name = "closes_at", nullable = false)
    private LocalDateTime closesAt;

    @Column(name = "max_winners", nullable = false)
    private Integer maxWinners;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ChallengeStatus status = ChallengeStatus.SCHEDULED;

    @Column(name = "current_entries", nullable = false)
    @Builder.Default
    private Integer currentEntries = 0;

    @Version
    private Long version;

    // 연관관계
    @OneToMany(mappedBy = "challenge", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChallengeEntry> entries = new ArrayList<>();

    // 비즈니스 로직
    public void open(LocalDateTime now) {

        if (status != ChallengeStatus.SCHEDULED)
            throw new IllegalStateException("오픈 가능한 상태가 아닙니다.");

        if (now.isBefore(opensAt)) {
            throw new IllegalStateException("아직 챌린지 시작 시간이 아닙니다.");
        }
        this.status = ChallengeStatus.ACTIVE;
    }

    public void close(LocalDateTime now) {

        if (status != ChallengeStatus.ACTIVE)
            throw new IllegalStateException("종료 가능한 상태가 아닙니다.");

        if (now.isBefore(closesAt)) {
            throw new IllegalStateException("아직 챌린지 종료 시간이 아닙니다");
        }
        this.status = ChallengeStatus.CLOSED;
    }

    public void join(LocalDateTime now) {
        if (status != ChallengeStatus.ACTIVE)
            throw new IllegalStateException("현재 참여할 수 있는 상태가 아닙니다");

        if (now.isBefore(opensAt)) {
            throw new IllegalStateException("아직 오픈 시간이 아닙니다");
        }

        if (!now.isBefore(closesAt)) {
            throw new IllegalStateException("이미 종료 되었습니다");
        }

        if (currentEntries >= maxWinners) {
            throw new IllegalStateException("인원이 초과되었습니다.");
        }
        currentEntries++;
    }

    public boolean canJoin(LocalDateTime now) {
        return status == ChallengeStatus.ACTIVE
                && !now.isBefore(opensAt)
                && now.isBefore(closesAt)
                && currentEntries < maxWinners;
    }
}
