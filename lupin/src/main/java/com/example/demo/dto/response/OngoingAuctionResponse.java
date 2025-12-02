package com.example.demo.dto.response;

import com.example.demo.domain.entity.Auction;
import java.time.LocalDateTime;

public record OngoingAuctionResponse(
        Long auctionId,
        String status,
        LocalDateTime startTime,
        LocalDateTime regularEndTime,
        Long currentPrice,
        Boolean overtimeStarted,
        LocalDateTime overtimeEndTime,
        Integer overtimeSeconds,
        AuctionItemResponse item
) {
    public static OngoingAuctionResponse from(Auction auction) {
        return new OngoingAuctionResponse(
                auction.getId(),
                auction.getStatus().name(),
                auction.getStartTime(),
                auction.getRegularEndTime(),
                auction.getCurrentPrice(),
                auction.getOvertimeStarted(),
                auction.getOvertimeEndTime(),
                auction.getOvertimeSeconds(),
                AuctionItemResponse.from(auction.getAuctionItem())
        );
    }
}

