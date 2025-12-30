package com.example.demo.domain.entity;

import com.example.demo.domain.enums.BidStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 경매 입찰 엔티티
 * 사용자의 입찰 내역 및 상태 관리
 */
@Entity
@Table(name = "auction_bids", indexes = {
    @Index(name = "idx_bid_auction", columnList = "auction_id"),
    @Index(name = "idx_bid_user", columnList = "user_id"),
    @Index(name = "idx_bid_status", columnList = "status"),
    @Index(name = "idx_bid_time", columnList = "bid_time"),
    @Index(name = "idx_bid_auction_user", columnList = "auction_id, user_id"),
    @Index(name = "idx_bid_ranking", columnList = "auction_id, status, bid_amount DESC, bid_time DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuctionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_bid_id")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @Column(nullable = false)
    private Long bidAmount;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime bidTime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BidStatus status = BidStatus.ACTIVE;

    public static AuctionBid of(Auction auction, User user, Long amount, LocalDateTime time) {
        return AuctionBid.builder()
                .auction(auction)
                .user(user)
                .bidAmount(amount)
                .bidTime(time)
                .status(BidStatus.ACTIVE)
                .build();
    }


    public boolean isActive() {
        return status == BidStatus.ACTIVE;
    }
    public boolean isOutBid() {
        return status == BidStatus.OUTBID;
    }

    public boolean isWinning() {
        return status == BidStatus.WINNING;
    }

    public boolean isLost() {
        return status  == BidStatus.LOST;
    }

    public boolean isRefunded() {
        return status == BidStatus.REFUNDED;
    }

    public void activateBid() {
        status = BidStatus.ACTIVE;
    }


    public void winBid() {
        if (!isActive()) {
            throw new IllegalStateException("최고가만 낙찰 가능합니다");
        }
        status = BidStatus.WINNING;
    }

    public void outBid(){
        if (!isActive()) {
            throw new IllegalStateException("활성화 상태에서만 outbid가 됩니다.");
        }
        status = BidStatus.OUTBID;
    }

    public void lostBid() {
        if (status != BidStatus.OUTBID) {
            throw new IllegalStateException("OUTBID 상태에서만 Lost가 가능합니다");
        }
        status = BidStatus.LOST;
    }

}
