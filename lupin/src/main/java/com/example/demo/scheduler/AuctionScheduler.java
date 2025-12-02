package com.example.demo.scheduler;

import com.example.demo.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionService auctionService;

    @Scheduled(cron = "0 * * * * *") // 매 분 0초마다
    public void handleAuctions() {
        LocalDateTime now = LocalDateTime.now();
        auctionService.activateScheduledAuctions(now);
        auctionService.closeExpiredAuctions(now);
    }

}
