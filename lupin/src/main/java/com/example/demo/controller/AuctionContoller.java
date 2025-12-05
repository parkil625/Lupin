package com.example.demo.controller;

import com.example.demo.dto.response.OngoingAuctionResponse;
import com.example.demo.dto.response.ScheduledAuctionResponse;
import com.example.demo.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auction")
@RequiredArgsConstructor
public class AuctionContoller {
    private final AuctionService auctionService;

    // 1. 진행 중인 경매 조회 (단건 반환)
    @GetMapping("/active")
    public ResponseEntity<OngoingAuctionResponse> getOngoingAuction() {
        // 보여주신 서비스 메서드를 호출합니다.
        return ResponseEntity.ok(auctionService.getOngoingAuctionWithItem());
    }

    @GetMapping("/scheduled")
    public ResponseEntity<List<ScheduledAuctionResponse>> getScheduledAuction() {
        // 보여주신 서비스 메서드를 호출합니다.
        return ResponseEntity.ok(auctionService.scheduledAuctionWithItem());
    }



}
