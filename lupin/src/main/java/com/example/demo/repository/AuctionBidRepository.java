package com.example.demo.repository;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import com.example.demo.dto.response.AuctionBidResponse;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Optional;

public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {

    //현재 최고가 찾기

    Optional<AuctionBid> findTopByAuctionAndStatusOrderByBidAmountDescBidTimeDesc(
            Auction auction,
            BidStatus status
    );

    List<AuctionBid> findByAuctionId(Long id);

    @EntityGraph(attributePaths = {"user"})
    List<AuctionBid> findTop5ByAuction_StatusOrderByBidAmountDesc(AuctionStatus status);
}
