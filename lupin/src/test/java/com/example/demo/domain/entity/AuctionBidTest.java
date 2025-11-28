package com.example.demo.domain.entity;

import com.example.demo.domain.enums.AuctionStatus;
import com.example.demo.domain.enums.BidStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AuctionBidTest {

    @Test
    void 입찰_상태변경_winning(){
        AuctionBid auctionBid = AuctionBid.builder()
                .bidAmount(1000L)
                .bidTime(LocalDateTime.now())
                .status(BidStatus.ACTIVE)
                .build();

        auctionBid.winBid();
    }

    @Test
    void 입찰_상태변경_outbid(){
        AuctionBid auctionBid = AuctionBid.builder()
                .bidAmount(1000L)
                .bidTime(LocalDateTime.now())
                .status(BidStatus.ACTIVE)
                .build();

        auctionBid.outBid();
    }

    @Test
    void 입찰_상태변경_lost(){
        AuctionBid auctionBid = AuctionBid.builder()
                .bidAmount(1000L)
                .bidTime(LocalDateTime.now())
                .status(BidStatus.OUTBID)
                .build();

        auctionBid.lostBid();
    }


}