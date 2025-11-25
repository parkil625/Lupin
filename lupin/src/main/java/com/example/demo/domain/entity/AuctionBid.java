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
    @Index(name = "idx_bid_auction", columnList = "auctionId"),
    @Index(name = "idx_bid_user", columnList = "userId"),
    @Index(name = "idx_bid_status", columnList = "status"),
    @Index(name = "idx_bid_time", columnList = "bidTime"),
    @Index(name = "idx_bid_auction_user", columnList = "auctionId, userId")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuctionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long auctionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long bidAmount;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime bidTime = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BidStatus status = BidStatus.ACTIVE;

    @Version
    private Long version;

    /**
     * 팩토리 메서드 - 입찰 생성
     */
    public static AuctionBid of(Long auctionId, Long userId, Long bidAmount) {
        return AuctionBid.builder()
                .auctionId(auctionId)
                .userId(userId)
                .bidAmount(bidAmount)
                .build();
    }

    /**
     * 다른 입찰에게 밀림 (OUTBID)
     */
    public void markAsOutbid() {
        if (this.status != BidStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태의 입찰만 OUTBID 처리할 수 있습니다.");
        }
        this.status = BidStatus.OUTBID;
    }

    /**
     * 경매 종료 - 낙찰
     */
    public void markAsWinning() {
        if (this.status != BidStatus.ACTIVE) {
            throw new IllegalStateException("ACTIVE 상태의 입찰만 낙찰 처리할 수 있습니다.");
        }
        this.status = BidStatus.WINNING;
    }

    /**
     * 경매 종료 - 낙찰 실패
     */
    public void markAsLost() {
        if (this.status == BidStatus.WINNING) {
            throw new IllegalStateException("낙찰된 입찰은 실패 처리할 수 없습니다.");
        }
        this.status = BidStatus.LOST;
    }

    /**
     * 환불 완료
     */
    public void markAsRefunded() {
        this.status = BidStatus.REFUNDED;
    }

    /**
     * 환불 필요 여부
     */
    public boolean needsRefund() {
        return status == BidStatus.OUTBID || status == BidStatus.LOST;
    }

    /**
     * 현재 최고가 입찰인지 확인
     */
    public boolean isActive() {
        return status == BidStatus.ACTIVE;
    }
}
