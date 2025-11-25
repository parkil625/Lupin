package com.example.demo.repository;

import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.enums.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 경매 입찰 리포지토리
 */
public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {

    /**
     * 경매별 입찰 내역 조회 (최신순)
     */
    List<AuctionBid> findByAuctionIdOrderByBidTimeDesc(Long auctionId);

    /**
     * 사용자별 입찰 내역 조회 (최신순)
     */
    List<AuctionBid> findByUserIdOrderByBidTimeDesc(Long userId);

    /**
     * 경매의 현재 최고가 입찰 조회
     */
    @Query("SELECT b FROM AuctionBid b WHERE b.auctionId = :auctionId AND b.status = 'ACTIVE' ORDER BY b.bidAmount DESC")
    Optional<AuctionBid> findActiveBidByAuctionId(@Param("auctionId") Long auctionId);

    /**
     * 경매의 모든 ACTIVE 입찰 조회 (OUTBID 처리용)
     */
    List<AuctionBid> findByAuctionIdAndStatus(Long auctionId, BidStatus status);

    /**
     * 사용자의 특정 경매 입찰 내역 조회
     */
    List<AuctionBid> findByAuctionIdAndUserId(Long auctionId, Long userId);

    /**
     * 환불 필요한 입찰 조회
     */
    @Query("SELECT b FROM AuctionBid b WHERE b.status IN ('OUTBID', 'LOST') AND b.auctionId = :auctionId")
    List<AuctionBid> findBidsNeedingRefund(@Param("auctionId") Long auctionId);
}
