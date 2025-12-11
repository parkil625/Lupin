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
            // 1. 현재 살아있는 1등 표(ACTIVE)라면 ->우승(WINNING)
            if (bid.isActive()) {
                bid.winBid();
            }
            // 2. 이미 밀려난 표(OUTBID)라면 ->패배(LOST)
            else if (bid.isOutBid()) {
                bid.lostBid();
            }
            // 그 외(이미 LOST 등)는 건드리지 않음
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
    public void placeBid(User user, Long bidAmount, LocalDateTime bidTime) {

        // 1. 경매 상태 확인
        if (this.status != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("경매가 종료되었습니다.");
        }

        // 2. 시간 검증 및 초읽기 진입 로직 (Logic B 적용)
        if (overtimeStarted) {
            // [상황 A] 이미 초읽기 모드인 경우
            // 초읽기 종료 시간과 비교
            if (bidTime.isAfter(overtimeEndTime)) {
                throw new IllegalStateException("초읽기 시간이 종료되었습니다.");
            }
            // 입찰 성공 -> 시간 30초 리셋 (연장)
            this.overtimeEndTime = bidTime.plusSeconds(overtimeSeconds);

        } else {
            // [상황 B] 아직 초읽기 전 (정규 시간 중 이거나 정규 시간 끝난 직후)

            if (bidTime.isBefore(regularEndTime)) {
                // B-1) 정규 시간 내 입찰
                // -> [중요] 아무런 시간 연장도 하지 않고, 초읽기 모드도 켜지 않습니다.
                // 그냥 입찰만 받아줍니다.
            } else {
                // B-2) 정규 시간이 지났음 (30초 대기 시간)
                // 정규 종료 후 30초까지만 기회를 줌
                LocalDateTime hardLimit = regularEndTime.plusSeconds(overtimeSeconds);

                if (bidTime.isAfter(hardLimit)) {
                    throw new IllegalStateException("정규 시간 및 추가 대기 시간이 모두 종료되었습니다.");
                }

                // 이때 비로소 "초읽기 모드" 시작!
                startOvertime(bidTime);
            }
        }

        // 3. 금액 검증 (기존 로직 유지)
        validateBid(bidAmount);

        // 4. 우승자 및 가격 업데이트
        currentPrice = bidAmount;
        winner = user;
        this.totalBids++;
    }

    // 경매 시간 상태 바꾸는 메소드
    public void startOvertime(LocalDateTime now) {
        overtimeStarted = true;
        overtimeEndTime = now.plusSeconds(overtimeSeconds);
    }

    //경매 입찰 생성 구문
    public AuctionBid createBid(User user,Long bidAmount, LocalDateTime bidTime ) {

        return AuctionBid.of(this, user, bidAmount, bidTime);
    }

    public LocalDateTime getEndTime() {
        // 초읽기가 시작되었고, 초읽기 종료 시간이 설정되어 있다면 -> 초읽기 종료 시간 반환
        if (Boolean.TRUE.equals(overtimeStarted) && overtimeEndTime != null) {
            return overtimeEndTime;
        }

        // 그 외에는 원래 정해진 정규 종료 시간 반환
        return regularEndTime.plusSeconds(overtimeSeconds);
    }
}
