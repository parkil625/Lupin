package com.example.demo.repository;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.domain.enums.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;

import java.util.List;

public interface AuctionRepository extends JpaRepository<Auction, Long> {


    List<Auction> findAll();

    AuctionBid findById(long id);



}
