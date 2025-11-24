package com.example.demo.domain.entity;

import com.example.demo.domain.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lottery_tickets", indexes = {
    @Index(name = "idx_lottery_ticket_user", columnList = "userId"),
    @Index(name = "idx_lottery_ticket_status", columnList = "status"),
    @Index(name = "idx_lottery_ticket_challenge", columnList = "challengeId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LotteryTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, insertable = false, updatable = false)
    private Long userId;

    @Column
    private Long challengeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    @Setter
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.AVAILABLE;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime issuedAt = LocalDateTime.now();

    @Column
    private LocalDateTime usedAt;

    // 동시성 제어 - 티켓 이중 사용 방지
    @Version
    private Long version;

    public void use(Long challengeId) {
        if (this.status != TicketStatus.AVAILABLE) {
            throw new IllegalStateException("이미 사용된 추첨권입니다.");
        }
        this.status = TicketStatus.USED;
        this.challengeId = challengeId;
        this.usedAt = LocalDateTime.now();
    }

    public void expire() {
        if (this.status == TicketStatus.AVAILABLE) {
            this.status = TicketStatus.EXPIRED;
        }
    }
}
