package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lottery_ticket")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LotteryTicket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "is_used", nullable = false, length = 1)
    @Builder.Default
    private String isUsed = "N"; // Y/N

    @Column(name = "win_result", length = 50)
    private String winResult;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 비즈니스 로직
    public void use(String result) {
        if ("Y".equals(this.isUsed)) {
            throw new IllegalStateException("이미 사용된 추첨권입니다.");
        }
        this.isUsed = "Y";
        this.winResult = result;
        this.usedAt = LocalDateTime.now();
    }
}
