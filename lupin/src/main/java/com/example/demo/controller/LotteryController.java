package com.example.demo.controller;

import com.example.demo.domain.entity.LotteryTicket;
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
}
