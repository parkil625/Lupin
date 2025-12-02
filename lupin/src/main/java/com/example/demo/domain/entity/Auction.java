package com.example.demo.domain.entity;

import com.example.demo.domain.enums.AuctionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 경매 엔티티
 * 매일 밤 10시 진행, 현재 포인트(게임머니)로 입찰
 */
@Entity
@Table(name = "auctions", indexes = {
    @Index(name = "idx_auction_status", columnList = "status"),
    @Index(name = "idx_auction_start_time", columnList = "start_time"),
    @Index(name = "idx_auction_end_time", columnList = "regular_end_time")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_id")
    private Long id;

    /**
     * 현재가 (항상 0P부터 시작, 1P 단위로 입찰 가능)
     */
    @Column(nullable = false)
    @Builder.Default
    private Long currentPrice = 0L;

    @Column(name = "start_time",nullable = false)
    private LocalDateTime startTime;

    /**
     * 정규 종료 시간 (기본 시간)
     * 이 시간까지는 정규 경매 진행
     */
    @Column(name = "regular_end_time",nullable = false)
    private LocalDateTime regularEndTime;

    /**
     * 초읽기 시작 여부
     * regularEndTime 이후 첫 입찰 시 true로 변경
     */
    @Column(name = "over_time_started", nullable = false)
    @Builder.Default
    private Boolean overtimeStarted = false;

    /**
     * 초읽기 종료 시간
     * 초읽기 모드에서 입찰 시마다 리셋됨 (30초)
     */
    @Column(name = "over_time_end_time")
    private LocalDateTime overtimeEndTime;

    /**
     * 초읽기 시간 (초) - 기본 30초
     */
    @Column(name ="over_time_seconds", nullable = false)
    @Builder.Default
    private Integer overtimeSeconds = 30;

    @Enumerated(EnumType.STRING)
    @Column(name ="status", nullable = false, length = 20)
    @Builder.Default
    private AuctionStatus status = AuctionStatus.SCHEDULED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;

    @Column(name = "winning_bid")
    private Long winningBid;

    @Column(name="total_bids", nullable = false)
    @Builder.Default
    private Integer totalBids = 0;

    @OneToOne(mappedBy = "auction", fetch = FetchType.LAZY)
    private AuctionItem auctionItem;





    /*
    * 편의 메소드
    *
    * */

    public boolean isScheduled() {
        return status == AuctionStatus.SCHEDULED;
    }

    public boolean isActive() {
        return status == AuctionStatus.ACTIVE;
    }

    public boolean isEnded() {
        return status == AuctionStatus.ENDED;
    }

    public boolean isCancelled() {
        return status == AuctionStatus.CANCELLED;
    }

    /*
    * 상태 전환 메소드
    * */

    public void activate(LocalDateTime now) {
        if (!isScheduled()) {
            throw new IllegalStateException("Scheduled 상태에서만 활성화가 가능합니다");
        }
        if (now.isBefore(startTime)) {
            throw new IllegalStateException("시작시간 전에는 활성화할 수 없습니다.");
        }
        if (now.isAfter(regularEndTime)) {
            throw new IllegalStateException("정규 종료 시간이 지난 경매는 활성화할 수 없습니다.");
        }
        this.status = AuctionStatus.ACTIVE;
    }
    public void deactivate(List<AuctionBid> bids) {

        if (!isActive()) {
            throw new IllegalStateException("Active 상태에서만 종료할 수 있습니다");
        }

        if (this.winner == null) {
            throw new IllegalStateException("winner가 없는 경매는 종료할 수 없습니다.");
        }

        for (AuctionBid bid : bids) {
            if (bid.getUser().equals(this.winner)) {
                bid.winBid();
            } else {
                bid.lostBid();
            }
        }

        this.overtimeStarted = false;
        this.status = AuctionStatus.ENDED;
    }

    public void cancel() {
        this.status = AuctionStatus.CANCELLED;
    }

    /*경매 입찰 확인 메소드*/

    public void validateTime(LocalDateTime bidTime) {

        if (!isActive()) {
            throw new IllegalStateException("경매가 활성 상태가 아닙니다.");
        }

        // 1) 정규시간 동안
        if (!overtimeStarted) {
            if (bidTime.isBefore(startTime) || bidTime.isAfter(regularEndTime)) {
                throw new IllegalStateException("정규 경매 시간에만 입찰할 수 있습니다.");
            }
            return;
        }

        // 2) 초읽기 모드
        if (overtimeStarted) {
            if (bidTime.isAfter(overtimeEndTime)) {
                throw new IllegalStateException("초읽기 시간이 종료되었습니다.");
            }
        }
    }


    public void validateBid(Long bidAmount) {
        if(bidAmount == null || bidAmount <= currentPrice) {
            throw new IllegalStateException("현재가 보다 높아야 입찰 가능합니다");
        }

    }

    //입찰시 확인 최고 낙찰자 교체(근데 원래 있던 사람 상태는 못바꿔줌)
    public void placeBid(User user,Long bidAmount,LocalDateTime bidTime ) {

        validateTime(bidTime);
        validateBid(bidAmount);

        currentPrice = bidAmount;
        winner = user;

        if (overtimeStarted) {
            this.overtimeEndTime = bidTime.plusSeconds(overtimeSeconds); // 반드시 bidTime 기준이어야 한다
        }
    }

    // 경매 시간 상태 바꾸는 메소드
    public void startOvertime(LocalDateTime now) {
        if (overtimeStarted) {
            throw new IllegalStateException("이미 초읽기가 시작된 상태입니다.");
        }
        if (now.isBefore(regularEndTime)) {
            throw new IllegalStateException("정규 시간이 종료되야 초읽기 모드가 가능합니다.");
        }
        overtimeStarted = true;
        overtimeEndTime = now.plusSeconds(overtimeSeconds);
    }

    //경매 입찰 생성 구문
    public AuctionBid createBid(User user,Long bidAmount, LocalDateTime bidTime ) {

        return AuctionBid.of(this, user, bidAmount, bidTime);
    }
}
