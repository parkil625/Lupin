package com.example.demo.controller;

import com.example.demo.domain.entity.Auction;
import com.example.demo.domain.entity.AuctionBid;
import com.example.demo.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 경매 컨트롤러
 */
@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AuctionController {

    private final AuctionService auctionService;

    /**
     * 활성 경매 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<Auction>> getActiveAuctions() {
        List<Auction> auctions = auctionService.getActiveAuctions();
        return ResponseEntity.ok(auctions);
    }

    /**
     * 경매 상세 조회
     */
    @GetMapping("/{auctionId}")
    public ResponseEntity<Auction> getAuction(@PathVariable Long auctionId) {
        Auction auction = auctionService.getAuction(auctionId);
        return ResponseEntity.ok(auction);
    }

    /**
     * 경매 입찰 내역 조회
     */
    @GetMapping("/{auctionId}/bids")
    public ResponseEntity<List<AuctionBid>> getBidHistory(@PathVariable Long auctionId) {
        List<AuctionBid> bids = auctionService.getBidHistory(auctionId);
        return ResponseEntity.ok(bids);
    }

    /**
     * 입찰하기
     */
    @PostMapping("/{auctionId}/bid")
    public ResponseEntity<Map<String, Object>> placeBid(
            @PathVariable Long auctionId,
            @RequestBody BidRequest request) {
        try {
            AuctionBid bid = auctionService.placeBid(
                    auctionId,
                    request.getUserId(),
                    request.getBidAmount()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("bid", bid);
            response.put("message", "입찰이 완료되었습니다.");

            return ResponseEntity.ok(response);
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.error("입찰 실패 - auctionId: {}, userId: {}, bidAmount: {}",
                    auctionId, request.getUserId(), request.getBidAmount(), e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 시청자 등록
     */
    @PostMapping("/{auctionId}/viewers")
    public ResponseEntity<Void> registerViewer(
            @PathVariable Long auctionId,
            @RequestBody ViewerRequest request) {
        auctionService.registerViewer(auctionId, request.getUserId());
        return ResponseEntity.ok().build();
    }

    /**
     * 시청자 수 조회
     */
    @GetMapping("/{auctionId}/viewers/count")
    public ResponseEntity<Map<String, Long>> getViewerCount(@PathVariable Long auctionId) {
        Long count = auctionService.getViewerCount(auctionId);

        Map<String, Long> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    // DTO Classes
    public static class BidRequest {
        private Long userId;
        private Long bidAmount;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getBidAmount() {
            return bidAmount;
        }

        public void setBidAmount(Long bidAmount) {
            this.bidAmount = bidAmount;
        }
    }

    public static class ViewerRequest {
        private Long userId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }
    }
}
