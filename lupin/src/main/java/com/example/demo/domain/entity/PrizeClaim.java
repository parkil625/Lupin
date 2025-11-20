package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 상금 수령 정보 엔티티
 */
@Entity
@Table(name = "prize_claim")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PrizeClaim extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_name", nullable = false, length = 50)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_holder", nullable = false, length = 50)
    private String accountHolder;

    @Column(name = "prize_amount", nullable = false, length = 50)
    private String prizeAmount; // "100만원", "50만원"

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING"; // PENDING, COMPLETED

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lottery_ticket_id", nullable = false)
    private LotteryTicket lotteryTicket;

    // 편의 메서드
    public void setUser(User user) {
        this.user = user;
    }

    public void setLotteryTicket(LotteryTicket ticket) {
        this.lotteryTicket = ticket;
    }

    public void markAsCompleted() {
        this.status = "COMPLETED";
    }
}
