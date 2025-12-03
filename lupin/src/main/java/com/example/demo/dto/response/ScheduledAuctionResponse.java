package com.example.demo.dto.response;

import com.example.demo.domain.entity.Auction;

import java.time.LocalDateTime;

public record ScheduledAuctionResponse(
        Long auctionId,
        LocalDateTime startTime,
        LocalDateTime regularEndTime,
        AuctionItemResponse item
) {
    public static ScheduledAuctionResponse from (Auction auction) {
        return new ScheduledAuctionResponse(
                auction.getId(),
                auction.getStartTime(),
                auction.getRegularEndTime(),
                AuctionItemResponse.from(auction.getAuctionItem())
        );
    }

}
