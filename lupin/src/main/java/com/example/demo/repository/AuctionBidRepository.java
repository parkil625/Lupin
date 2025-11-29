package com.example.demo.repository;

import com.example.demo.domain.entity.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionBidRepository extends JpaRepository<AuctionBid, Long> {
}
