package com.example.demo.controller;

import com.example.demo.domain.entity.User;
import com.example.demo.dto.request.AuctionRequest;
import com.example.demo.dto.response.AuctionBidResponse;
import com.example.demo.dto.response.OngoingAuctionResponse;
import com.example.demo.dto.response.ScheduledAuctionResponse;
import com.example.demo.security.CurrentUser;
import com.example.demo.service.AuctionService;
import com.example.demo.service.AuctionSseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auction")
@RequiredArgsConstructor
@Tag(name = "Auction Controller", description = "경매 관련 API")
public class AuctionController {

    private final AuctionService auctionService;

    private final AuctionSseService auctionSseService;

    @Operation(summary = "진행 중인 경매 조회", description = "현재 진행 중인 경매 정보를 조회합니다.")
    @GetMapping("/active")
    public ResponseEntity<OngoingAuctionResponse> getOngoingAuction() {
        return ResponseEntity.ok(auctionService.getOngoingAuctionWithItem());
    }

    @Operation(summary = "예정된 경매 조회", description = "진행 예정인 경매 목록을 조회합니다.")
    @GetMapping("/scheduled")
    public ResponseEntity<List<ScheduledAuctionResponse>> getScheduledAuction() {
        return ResponseEntity.ok(auctionService.scheduledAuctionWithItem());
    }

    @Operation(summary = "경매 입찰", description = "경매에 포인트를 사용하여 입찰합니다. (Redis 분산 락 적용)")
    @PostMapping("/{auctionId}/bid")
    public ResponseEntity<Void> placeBid(
            @PathVariable Long auctionId,
            @RequestBody AuctionRequest request,
            @CurrentUser User user
    ) {

        // 서비스 호출 (입찰 시간은 서버 시간 기준)
        auctionService.placeBid(auctionId, user.getId(), request.getBidAmount(), LocalDateTime.now());

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "진행 중인 경매 입찰 기록", description = "현재 ACTIVE 상태인 경매의 입찰 내역을 가격순으로 조회합니다.")
    @GetMapping("/active/history") // 경로 변경: /{auctionId}/history -> /active/history
    public ResponseEntity<List<AuctionBidResponse>> getBidHistory() {
        return ResponseEntity.ok(auctionService.getAuctionStatus());
    }

    @GetMapping(value = "/stream/{auctionId}", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamAuction(@PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionSseService.subscribe(auctionId));
    }

}