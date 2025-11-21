package com.example.demo.scheduler;

import com.example.demo.domain.entity.LotteryTicket;
import com.example.demo.domain.entity.PrizeClaim;
import com.example.demo.domain.enums.PrizeType;
import com.example.demo.repository.LotteryTicketRepository;
import com.example.demo.repository.PrizeClaimRepository;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 매일 자정에 추첨을 진행하는 스케줄러
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LotteryScheduler {

    private final LotteryTicketRepository lotteryTicketRepository;
    private final PrizeClaimRepository prizeClaimRepository;
    private final NotificationService notificationService;

    /**
     * 매일 자정(0시)에 자동 추첨 진행
     * - 1등: 1명 (100만원)
     * - 2등: 2명 (50만원)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void runDailyLottery() {
        log.info("=== 일일 자동 추첨 시작 ===");

        List<LotteryTicket> tickets = lotteryTicketRepository.findAll();

        if (tickets.isEmpty()) {
            log.info("추첨권이 없습니다.");
            return;
        }

        int totalTickets = tickets.size();
        log.info("총 추첨권: {}장", totalTickets);

        List<LotteryTicket> pool = new ArrayList<>(tickets);
        Random random = new Random();
        Set<Long> winnerUserIds = new HashSet<>();

        int firstPlaceCount = 0;
        int secondPlaceCount = 0;

        // 1등 추첨 (1명)
        if (!pool.isEmpty()) {
            int winnerIndex = random.nextInt(pool.size());
            LotteryTicket winner = pool.get(winnerIndex);
            Long winnerUserId = winner.getUser().getId();

            PrizeClaim prizeClaim = PrizeClaim.builder()
                    .prizeType(PrizeType.FIRST_PLACE)
                    .build();
            prizeClaim.setUser(winner.getUser());
            prizeClaimRepository.save(prizeClaim);

            notificationService.createSystemNotification(
                winnerUserId,
                "🎉 축하합니다! 1등 당첨 (100만원)"
            );

            log.info("1등 당첨 - userId: {}, userName: {}",
                winnerUserId, winner.getUser().getRealName());

            winnerUserIds.add(winnerUserId);
            pool.removeIf(t -> t.getUser().getId().equals(winnerUserId));
            firstPlaceCount++;
        }

        // 2등 추첨 (2명)
        for (int i = 0; i < 2 && !pool.isEmpty(); i++) {
            int winnerIndex = random.nextInt(pool.size());
            LotteryTicket winner = pool.get(winnerIndex);
            Long winnerUserId = winner.getUser().getId();

            PrizeClaim prizeClaim = PrizeClaim.builder()
                    .prizeType(PrizeType.SECOND_PLACE)
                    .build();
            prizeClaim.setUser(winner.getUser());
            prizeClaimRepository.save(prizeClaim);

            notificationService.createSystemNotification(
                winnerUserId,
                "🎉 축하합니다! 2등 당첨 (50만원)"
            );

            log.info("2등 당첨 - userId: {}, userName: {}",
                winnerUserId, winner.getUser().getRealName());

            winnerUserIds.add(winnerUserId);
            pool.removeIf(t -> t.getUser().getId().equals(winnerUserId));
            secondPlaceCount++;
        }

        // 낙첨자 알림 (중복 제거된 userId)
        Set<Long> loserUserIds = tickets.stream()
                .map(t -> t.getUser().getId())
                .filter(userId -> !winnerUserIds.contains(userId))
                .collect(Collectors.toSet());

        for (Long userId : loserUserIds) {
            notificationService.createSystemNotification(
                userId,
                "아쉽게도 이번 추첨에서 당첨되지 않았습니다. 다음 기회를 노려주세요!"
            );
        }

        lotteryTicketRepository.deleteAll();

        log.info("=== 일일 자동 추첨 완료 ===");
        log.info("1등: {}명, 2등: {}명, 낙첨: {}명",
            firstPlaceCount, secondPlaceCount, loserUserIds.size());
    }
}
