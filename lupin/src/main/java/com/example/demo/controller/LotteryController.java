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
     * 추첨권 발급
     */
    @PostMapping("/issue")
    public ResponseEntity<LotteryTicket> issueTicket(@RequestParam Long userId) {
        LotteryTicket ticket = lotteryService.issueTicket(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    /**
     * 사용자의 추첨권 조회
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<List<LotteryTicket>> getTickets(@PathVariable Long userId) {
        List<LotteryTicket> tickets = lotteryService.getTickets(userId);
        return ResponseEntity.ok(tickets);
    }

    /**
     * 사용자의 추첨권 개수 조회
     */
    @GetMapping("/users/{userId}/count")
    public ResponseEntity<Map<String, Long>> countTickets(@PathVariable Long userId) {
        Long count = lotteryService.countTickets(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }
}
