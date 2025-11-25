package com.example.demo.scheduler;

import com.example.demo.service.AuctionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 경매 스케줄러
 * - 매일 밤 10시 경매 시작
 * - 1분마다 종료된 경매 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionScheduler {

    private final AuctionService auctionService;

    /**
     * 예정된 경매 시작 (1분마다 체크)
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void startScheduledAuctions() {
        try {
            auctionService.startScheduledAuctions();
        } catch (Exception e) {
            log.error("예정된 경매 시작 실패", e);
        }
    }

    /**
     * 종료된 경매 처리 (1분마다 체크)
     */
    @Scheduled(fixedRate = 60000) // 1분마다
    public void endExpiredAuctions() {
        try {
            auctionService.endExpiredAuctions();
        } catch (Exception e) {
            log.error("종료된 경매 처리 실패", e);
        }
    }

    /**
     * 매일 밤 10시 경매 시작 (cron 예시)
     * 실제 프로덕션에서는 경매 데이터를 미리 생성하고,
     * 이 시간에 SCHEDULED -> ACTIVE로 전환
     */
    @Scheduled(cron = "0 0 22 * * *") // 매일 22:00
    public void dailyAuctionStart() {
        log.info("매일 밤 10시 경매 시작 스케줄 실행");
        // 매일 밤 10시에 시작할 경매들은 미리 SCHEDULED 상태로 생성되어 있어야 함
        // startScheduledAuctions()가 이미 1분마다 실행되므로, 여기서는 로그만 남김
    }
}
