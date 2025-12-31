package com.example.demo.repository;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.dto.response.AuctionBidResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {

    //현재 최고가 찾기

    Optional<AuctionBid> findTopByAuctionAndStatusOrderByBidAmountDescBidTimeDesc(
            Auction auction,
            BidStatus status
    );

    List<AuctionBid> findByAuctionId(Long id);

    @Query("SELECT b FROM AuctionBid b JOIN FETCH b.user WHERE b.auction.status = com.example.demo.domain.enums.AuctionStatus.ACTIVE ORDER BY b.bidAmount DESC")
    List<AuctionBid> findBidsByActiveAuction();
}
