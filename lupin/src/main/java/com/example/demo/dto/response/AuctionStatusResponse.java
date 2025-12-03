package com.example.demo.dto.response;

import java.time.LocalDateTime;

public record AuctionStatusResponse(
        Long auctionId,
        Long currentPrice,
        String winnerName,       // 현재 1등의 이름 (User 객체 전체 X)
        Boolean overtimeStarted, // 초읽기 시작 여부
        LocalDateTime overtimeEndTime, // 초읽기 종료 시간
        Integer totalBids        // 총 입찰 횟수 (선택)
) {
}
