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

}
