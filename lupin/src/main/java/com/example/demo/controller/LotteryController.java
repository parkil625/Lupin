package com.example.demo.controller;

import com.example.demo.domain.entity.LotteryTicket;
import com.example.demo.domain.entity.PrizeClaim;
import com.example.demo.service.LotteryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 추첨권 관련 API
 */
@RestController
@RequestMapping("/api/lottery")
@RequiredArgsConstructor
public class LotteryController {

    private final LotteryService lotteryService;

    /**
     * 추첨권 구매
     */
    @PostMapping("/purchase")
    public ResponseEntity<LotteryTicket> purchaseTicket(@RequestParam Long userId) {
        LotteryTicket ticket = lotteryService.purchaseTicket(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    /**
     * 추첨권 사용 (추첨 진행)
     */
    @PostMapping("/tickets/{ticketId}/use")
    public ResponseEntity<Map<String, String>> useLotteryTicket(@PathVariable Long ticketId) {
        String result = lotteryService.useLotteryTicket(ticketId);
        return ResponseEntity.ok(Map.of("result", result));
    }

    /**
     * 사용자의 미사용 추첨권 조회
     */
    @GetMapping("/users/{userId}/unused")
    public ResponseEntity<List<LotteryTicket>> getUnusedTickets(@PathVariable Long userId) {
        List<LotteryTicket> tickets = lotteryService.getUnusedTickets(userId);
        return ResponseEntity.ok(tickets);
    }

    /**
     * 사용자의 미사용 추첨권 개수 조회
     */
    @GetMapping("/users/{userId}/unused/count")
    public ResponseEntity<Map<String, Long>> countUnusedTickets(@PathVariable Long userId) {
        Long count = lotteryService.countUnusedTickets(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 사용자의 모든 추첨권 조회
     */
    @GetMapping("/users/{userId}/all")
    public ResponseEntity<List<LotteryTicket>> getAllTickets(@PathVariable Long userId) {
        List<LotteryTicket> tickets = lotteryService.getAllTickets(userId);
        return ResponseEntity.ok(tickets);
    }

    /**
     * 상금 수령 신청 (은행 정보 입력)
     */
    @PostMapping("/tickets/{ticketId}/claim")
    public ResponseEntity<PrizeClaim> claimPrize(
            @PathVariable Long ticketId,
            @RequestParam String bankName,
            @RequestParam String accountNumber,
            @RequestParam String accountHolder) {
        PrizeClaim claim = lotteryService.claimPrize(ticketId, bankName, accountNumber, accountHolder);
        return ResponseEntity.status(HttpStatus.CREATED).body(claim);
    }

    /**
     * 사용자의 당첨 내역 조회
     */
    @GetMapping("/users/{userId}/wins")
    public ResponseEntity<List<LotteryTicket>> getWinningTickets(@PathVariable Long userId) {
        List<LotteryTicket> tickets = lotteryService.getWinningTickets(userId);
        return ResponseEntity.ok(tickets);
    }

    /**
     * 사용자의 상금 수령 신청 내역 조회
     */
    @GetMapping("/users/{userId}/claims")
    public ResponseEntity<List<PrizeClaim>> getPrizeClaims(@PathVariable Long userId) {
        List<PrizeClaim> claims = lotteryService.getPrizeClaims(userId);
        return ResponseEntity.ok(claims);
    }

    /**
     * 수동 추첨 실행 (테스트용)
     */
    @PostMapping("/draw")
    public ResponseEntity<Map<String, String>> runManualDraw() {
        lotteryService.runManualLottery();
        return ResponseEntity.ok(Map.of("result", "추첨 완료"));
    }
}
