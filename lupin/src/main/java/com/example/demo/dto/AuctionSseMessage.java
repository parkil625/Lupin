package com.example.demo.dto;

import lombok.*;

@Getter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionSseMessage {
    private Long auctionId;
    private Long currentPrice;
    private Long bidderId;
    private String bidderName; // 입찰자 이름 (내역 갱신용)
    private String bidTime;    // 입찰 시간 (내역 갱신용)
    private String newEndTime; // 연장된 마감 시간 (타이머 갱신용)
    private Integer totalBids;
}
