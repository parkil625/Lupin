package com.example.demo.scheduler;

import com.example.demo.domain.entity.LotteryTicket;
import com.example.demo.repository.LotteryTicketRepository;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 매일 자정에 추첨을 진행하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LotteryScheduler {

    private final LotteryTicketRepository lotteryTicketRepository;
    private final NotificationService notificationService;

    /**
     * 매일 자정(0시)에 자동 추첨 진행
     * - 1등: 1명 (100만원)
     * - 2등: 2명 (50만원)
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 0시 0분 0초
    @Transactional
    public void runDailyLottery() {
        log.info("=== 일일 자동 추첨 시작 ===");

        // 모든 미사용 추첨권 조회
        List<LotteryTicket> unusedTickets = lotteryTicketRepository.findByIsUsed("N");

        if (unusedTickets.isEmpty()) {
            log.info("미사용 추첨권이 없습니다.");
            return;
        }

        int totalTickets = unusedTickets.size();
        log.info("총 추첨권: {}장", totalTickets);

        // 추첨권 섞기
        List<LotteryTicket> shuffledTickets = new ArrayList<>(unusedTickets);
        Collections.shuffle(shuffledTickets);

        List<LotteryTicket> firstPlaceWinners = new ArrayList<>();
        List<LotteryTicket> secondPlaceWinners = new ArrayList<>();

        // 1등 추첨 (1명, 100만원)
        if (shuffledTickets.size() >= 1) {
            LotteryTicket winner = shuffledTickets.get(0);
            winner.use("1등_100만원");
            firstPlaceWinners.add(winner);

            // 당첨 알림 생성
            notificationService.createSystemNotification(
                winner.getUser().getId(),
                "🎉 축하합니다! 1등 당첨 (100만원)"
            );

            log.info("1등 당첨 - userId: {}, userName: {}",
                winner.getUser().getId(), winner.getUser().getRealName());
        }

        // 2등 추첨 (2명, 50만원)
        for (int i = 1; i < Math.min(3, shuffledTickets.size()); i++) {
            LotteryTicket winner = shuffledTickets.get(i);
            winner.use("2등_50만원");
            secondPlaceWinners.add(winner);

            // 당첨 알림 생성
            notificationService.createSystemNotification(
                winner.getUser().getId(),
                "🎉 축하합니다! 2등 당첨 (50만원)"
            );

            log.info("2등 당첨 - userId: {}, userName: {}",
                winner.getUser().getId(), winner.getUser().getRealName());
        }

        // 나머지 추첨권은 낙첨 처리
        for (int i = 3; i < shuffledTickets.size(); i++) {
            shuffledTickets.get(i).use("낙첨");
        }

        log.info("=== 일일 자동 추첨 완료 ===");
        log.info("1등: {}명, 2등: {}명, 낙첨: {}명",
            firstPlaceWinners.size(),
            secondPlaceWinners.size(),
            Math.max(0, totalTickets - firstPlaceWinners.size() - secondPlaceWinners.size()));
    }
}
