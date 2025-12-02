package com.example.demo.repository;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.enums.BidStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {

    //현재 최고가 찾기

    Optional<AuctionBid> findTopByAuctionAndStatusOrderByBidAmountDescBidTimeDesc(
            Auction auction,
            BidStatus status
    );

    List<AuctionBid> findByAuctionId(Long id);
}
