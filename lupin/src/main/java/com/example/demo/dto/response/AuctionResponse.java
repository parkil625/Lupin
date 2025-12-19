package com.example.demo.dto.response;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionItem;
import com.example.demo.domain.enums.AuctionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuctionResponse {

    // 1. 경매 기본 정보
    private Long auctionId;
    private AuctionStatus status;
    private Long currentPrice;
    private Integer totalBids;

    // 2. 시간 관련 정보
    private LocalDateTime startTime;
    private LocalDateTime regularEndTime;
    private Boolean overtimeStarted;
    private LocalDateTime overtimeEndTime;
    private Integer overtimeSeconds;

    // 3. 물품 정보 (프론트엔드의 'item' 객체와 대응)
    private AuctionItemInfo item;

    // 4. 낙찰자 이름 (명예의 전당용)
    private String winnerName;

    /**
     * Entity -> DTO 변환 메소드
     * (이걸 쓰면 Service 코드가 아주 깔끔해집니다)
     */
    public static AuctionResponse from(Auction auction, String winnerName) {
        return AuctionResponse.builder()
                .auctionId(auction.getId())
                .status(auction.getStatus())
                .currentPrice(auction.getCurrentPrice())
                .totalBids(auction.getTotalBids())
                .startTime(auction.getStartTime())
                .regularEndTime(auction.getRegularEndTime())
                .overtimeStarted(auction.getOvertimeStarted())
                .overtimeEndTime(auction.getOvertimeEndTime())
                .overtimeSeconds(auction.getOvertimeSeconds())
                .winnerName(winnerName) // 낙찰자 이름 주입
                .item(AuctionItemInfo.from(auction.getAuctionItem())) // 물품 정보 변환
                .build();
    }

    // 물품 정보를 담을 내부 클래스 (프론트엔드의 AuctionItemDetail 대응)
    @Getter
    @Builder
    public static class AuctionItemInfo {
        private Long itemId;
        private String itemName;
        private String description;
        private String imageUrl;

        public static AuctionItemInfo from(AuctionItem item) {
            if (item == null) return null;
            return AuctionItemInfo.builder()
                    .itemId(item.getId())
                    .itemName(item.getItemName())
                    .description(item.getDescription())
                    .imageUrl(item.getItemImage())
                    .build();
        }
    }
}