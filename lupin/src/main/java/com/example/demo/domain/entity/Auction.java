package com.example.demo.domain.entity;

import com.example.demo.domain.enums.AuctionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 경매 엔티티
 * 매일 밤 10시 진행, 현재 포인트(게임머니)로 입찰
 */
@Entity
@Table(name = "auctions", indexes = {
    @Index(name = "idx_auction_status", columnList = "status"),
    @Index(name = "idx_auction_start_time", columnList = "startTime"),
    @Index(name = "idx_auction_end_time", columnList = "endTime")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String itemName;

    @Column(length = 1000)
    private String description;

    /**
     * 현재가 (항상 0P부터 시작, 1P 단위로 입찰 가능)
     */
    @Column(nullable = false)
    @Builder.Default
    private Long currentPrice = 0L;

    @Column(nullable = false)
    private LocalDateTime startTime;

    /**
     * 정규 종료 시간 (기본 시간)
     * 이 시간까지는 정규 경매 진행
     */
    @Column(nullable = false)
    private LocalDateTime regularEndTime;

    /**
     * 초읽기 시작 여부
     * regularEndTime 이후 첫 입찰 시 true로 변경
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean overtimeStarted = false;

    /**
     * 초읽기 종료 시간
     * 초읽기 모드에서 입찰 시마다 리셋됨 (30초)
     */
    @Column
    private LocalDateTime overtimeEndTime;

    /**
     * 초읽기 시간 (초) - 기본 30초
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer overtimeSeconds = 30;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AuctionStatus status = AuctionStatus.SCHEDULED;

    @Column
    private Long winnerId;

    @Column
    private Long winningBid;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalBids = 0;

    @Version
    private Long version;

    /**
     * 입찰 가능 여부 확인
     */
    public boolean canBid(LocalDateTime now) {
        if (status != AuctionStatus.ACTIVE || now.isBefore(startTime)) {
            return false;
        }

        // 정규 시간 내
        if (now.isBefore(regularEndTime)) {
            return true;
        }

        // 초읽기 시작됨 + 초읽기 시간 내
        if (overtimeStarted && overtimeEndTime != null) {
            return now.isBefore(overtimeEndTime);
        }

        // 정규 시간 종료 후 아직 초읽기 시작 전
        return false;
    }

    /**
     * 정규 시간인지 확인
     */
    public boolean isRegularTime(LocalDateTime now) {
        return now.isBefore(regularEndTime);
    }

    /**
     * 초읽기 모드인지 확인
     */
    public boolean isOvertimeMode(LocalDateTime now) {
        return now.isAfter(regularEndTime) || overtimeStarted;
    }

    /**
     * 남은 시간 (초)
     */
    public long getRemainingSeconds(LocalDateTime now) {
        if (now.isBefore(regularEndTime)) {
            // 정규 시간
            return java.time.Duration.between(now, regularEndTime).getSeconds();
        } else if (overtimeStarted && overtimeEndTime != null) {
            // 초읽기 모드
            if (now.isBefore(overtimeEndTime)) {
                return java.time.Duration.between(now, overtimeEndTime).getSeconds();
            }
        }
        return 0;
    }

    /**
     * 입찰가 유효성 검증
     * 현재가보다 최소 1P 이상이면 OK
     */
    public boolean isValidBidAmount(Long bidAmount) {
        return bidAmount > currentPrice;
    }

    /**
     * 입찰 처리 (체스 초읽기 방식)
     * - 정규 시간: 그냥 입찰만 처리
     * - 정규 시간 종료 후: 초읽기 모드 진입 + 30초 설정
     * - 초읽기 모드: 30초 리셋
     */
    public void placeBid(Long userId, Long bidAmount, LocalDateTime now) {
        if (!canBid(now)) {
            throw new IllegalStateException("입찰 불가능한 경매입니다.");
        }
        if (!isValidBidAmount(bidAmount)) {
            throw new IllegalStateException("현재가보다 높은 금액을 입찰해주세요.");
        }

        this.currentPrice = bidAmount;
        this.winnerId = userId;
        this.totalBids++;

        // 초읽기 모드 처리
        if (isOvertimeMode(now)) {
            this.overtimeStarted = true;
            this.overtimeEndTime = now.plusSeconds(overtimeSeconds);
        }
    }

    /**
     * 경매 시작
     */
    public void start() {
        if (status != AuctionStatus.SCHEDULED) {
            throw new IllegalStateException("예정된 경매만 시작할 수 있습니다.");
        }
        this.status = AuctionStatus.ACTIVE;
        this.currentPrice = 0L;
        this.overtimeStarted = false;
        this.overtimeEndTime = null;
    }

    /**
     * 경매 종료
     */
    public void end() {
        if (status != AuctionStatus.ACTIVE) {
            throw new IllegalStateException("진행 중인 경매만 종료할 수 있습니다.");
        }
        this.status = AuctionStatus.ENDED;
        if (winnerId != null) {
            this.winningBid = currentPrice;
        }
    }

    /**
     * 경매 취소
     */
    public void cancel() {
        if (status == AuctionStatus.ENDED) {
            throw new IllegalStateException("종료된 경매는 취소할 수 없습니다.");
        }
        this.status = AuctionStatus.CANCELLED;
    }

    /**
     * 낙찰 여부
     */
    public boolean hasWinner() {
        return winnerId != null;
    }
}
