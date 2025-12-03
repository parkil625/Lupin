package com.example.demo.scheduler;

import com.example.demo.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

    private static final int RETENTION_DAYS = 15;

    private final NotificationRepository notificationRepository;

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시에 실행
    @Transactional
    public void cleanupOldNotifications() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
        int deletedCount = notificationRepository.deleteByCreatedAtBefore(cutoffDate);

        if (deletedCount > 0) {
            log.info("{}일 이상 된 알림 {}개 삭제 완료", RETENTION_DAYS, deletedCount);
        }
    }
}
