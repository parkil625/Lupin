package com.example.demo.domain.entity;

import com.example.demo.domain.enums.ChallengeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "challenges")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private LocalDateTime openTime;

    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    private ChallengeStatus status;

    /**
     * 챌린지 참가 가능 여부 확인
     */
    public boolean canJoin(LocalDateTime now) {
        return status == ChallengeStatus.OPEN &&
               now.isAfter(openTime) &&
               now.isBefore(endTime);
    }

    /**
     * 챌린지 열기
     */
    public void open(LocalDateTime now) {
        if (status != ChallengeStatus.SCHEDULED) {
            throw new IllegalStateException("예정된 챌린지만 열 수 있습니다.");
        }
        if (now.isBefore(openTime)) {
            throw new IllegalStateException("시작 시간이 되지 않았습니다.");
        }
        this.status = ChallengeStatus.OPEN;
    }

    /**
     * 챌린지 닫기
     */
    public void close(LocalDateTime now) {
        if (status != ChallengeStatus.OPEN) {
            throw new IllegalStateException("열린 챌린지만 닫을 수 있습니다.");
        }
        this.status = ChallengeStatus.CLOSED;
    }
}
