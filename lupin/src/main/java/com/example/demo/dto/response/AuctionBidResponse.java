package com.example.demo.dto.response;

import com.example.demo.domain.entity.AuctionBid;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class AuctionBidResponse {
    private Long id;
    private Long userId;
    private String userName; // 유저 이름 포함
    private Long bidAmount;
    private LocalDateTime bidTime;
    private String status;

    public static AuctionBidResponse from(AuctionBid bid) {
        return AuctionBidResponse.builder()
                .id(bid.getId())
                .userId(bid.getUser().getId())
                .userName(bid.getUser().getName())
                .bidAmount(bid.getBidAmount())
                .bidTime(bid.getBidTime())
                .status(bid.getStatus().name())
                .build();
    }
}
