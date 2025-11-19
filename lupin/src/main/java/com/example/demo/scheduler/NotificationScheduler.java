package com.example.demo.scheduler;

import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 알림 관련 스케줄러
 * - 15일 이상 된 알림 자동 삭제
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationRepository notificationRepository;

    /**
     * 매일 새벽 3시에 15일 이상 된 알림 삭제
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteOldNotifications() {
        log.info("=== 오래된 알림 삭제 스케줄러 시작 ===");

        LocalDateTime fifteenDaysAgo = LocalDateTime.now().minusDays(15);

        try {
            int deletedCount = notificationRepository.deleteByCreatedAtBefore(fifteenDaysAgo);
            log.info("15일 이상 된 알림 {}개 삭제 완료", deletedCount);
        } catch (Exception e) {
            log.error("알림 삭제 중 오류 발생", e);
        }

        log.info("=== 오래된 알림 삭제 스케줄러 종료 ===");
    }
}
